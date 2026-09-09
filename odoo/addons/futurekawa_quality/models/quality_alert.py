import json
import logging
import urllib.error
import urllib.request

from odoo import models, fields, api, _
from odoo.exceptions import UserError, ValidationError

_logger = logging.getLogger(__name__)

class QualityAlert(models.Model):
    _name = 'futurekawa.quality.alert'
    _description = 'Fiche de Non-Conformite Qualite FutureKawa'
    _inherit = ['mail.thread', 'mail.activity.mixin']
    _order = 'date_creation desc'

    name = fields.Char(
        string='Reference Alerte', required=True, copy=False,
        readonly=True, index=True, default=lambda self: 'New',
    )

    backend_alerte_id = fields.Integer(
        string='ID Alerte (Backend)', index=True, copy=False,
        help="Identifiant de l'alerte dans la base du backend local. "
             "Sert de cle d'idempotence pour eviter les doublons.",
    )
    entrepot_nom = fields.Char(string='Entrepot', required=True, tracking=True)
    pays_code = fields.Char(string='Code Pays', index=True, tracking=True)
    pays_id = fields.Many2one(
        'futurekawa.pays', string='Pays', tracking=True, ondelete='restrict',
        help="Resolu depuis pays_code a la creation ; porte l'URL du backend du pays.",
    )
    lot_reference = fields.Char(string='Reference Lot', tracking=True)

    type_anomaly = fields.Selection(
        selection=[
            ('condition_non_ideale', 'Conditions de stockage non ideales'),
            ('lot_trop_ancien', 'Lot trop ancien / perime'),
        ],
        string='Type Anomalie', required=True,
    )
    niveau = fields.Selection(
        selection=[
            ('info', 'Info'),
            ('warning', 'Avertissement'),
            ('critique', 'Critique'),
        ],
        string='Niveau', required=True, default='warning', tracking=True,
    )

    valeur_enregistree = fields.Float(string='Valeur Enregistree')
    valeur_cible = fields.Float(string='Valeur Cible (Seuil)')
    message_description = fields.Text(string='Description')

    date_creation = fields.Datetime(
        string='Date de Detection', default=fields.Datetime.now, readonly=True,
    )
    date_resolution = fields.Datetime(string='Date de Resolution', tracking=True)
    state = fields.Selection(
        selection=[
            ('draft', 'Detecte'),
            ('investigation', "En cours d'analyse"),
            ('resolved', 'Resolu'),
            ('rejected', 'Lot declasse / perdu'),
        ],
        string='Statut', default='draft', required=True, tracking=True,
    )
    notes_audit = fields.Text(string="Notes d'Audit Qualite")
    responsable_id = fields.Many2one('res.users', string='Responsable Qualite', tracking=True)

    _BACKEND_STATUT = {
        'draft': 'OUVERTE',
        'investigation': 'NOTIFIEE',
        'resolved': 'CLOTUREE',
        'rejected': 'CLOTUREE',
    }

    @api.constrains('backend_alerte_id', 'pays_code')
    def _check_backend_alerte_unique(self):
        """One ticket per backend alert and country.

        Not a SQL UNIQUE: Odoo stores an unset Integer as 0, so hand-created tickets
        would all collide on (0, country). Only pushed tickets are checked.
        """
        for record in self:
            if not record.backend_alerte_id:
                continue
            duplicate = self.search_count([
                ('id', '!=', record.id),
                ('backend_alerte_id', '=', record.backend_alerte_id),
                ('pays_code', '=', record.pays_code),
            ])
            if duplicate:
                raise ValidationError(_(
                    "A ticket already exists for backend alert %(id)s in %(pays)s."
                ) % {'id': record.backend_alerte_id, 'pays': record.pays_code})

    @api.model_create_multi
    def create(self, vals_list):
        noms_pays = []
        for vals in vals_list:
            noms_pays.append(vals.pop('pays_nom', None))
            if vals.get('name', 'New') == 'New':
                vals['name'] = self.env['ir.sequence'].next_by_code(
                    'futurekawa.quality.alert') or 'ALT/'
        records = super().create(vals_list)
        records._link_pays(noms_pays)
        records._notify_quality_team()
        return records

    def _link_pays(self, noms_pays=None):
        """Attach each ticket to its country record, creating it if the code is new.

        `noms_pays` carries the country names the backends sent, positionally, so a
        freshly discovered country is created with a readable name instead of its bare
        code. Its backend URL still has to be filled in by an administrator
        (FutureKawa Quality > Configuration > Pays) before decisions can be pushed back.
        """
        pays_model = self.env['futurekawa.pays'].sudo()
        for index, record in enumerate(self):
            if not record.pays_code or record.pays_id:
                continue
            nom = None
            if noms_pays and index < len(noms_pays):
                nom = noms_pays[index]
            record.pays_id = pays_model._get_or_create(record.pays_code, nom)

    def _notify_quality_team(self):
        """Email the quality team when an alert ticket is created.

        Recipients are managed entirely in Odoo: they are resolved at send time
        from the Contacts tagged "Responsable Qualite FutureKawa", so the list is
        changed by tagging or untagging a contact in the Contacts app. No address
        is stored in source, and the backend sends none.

        There is no filter on `niveau`: the cahier des charges (III.4) asks for a
        notification as soon as storage conditions leave the acceptable range, and
        the backend only escalates to `critique` beyond twice the tolerance.

        Failures must never block ticket creation (the backend creates these
        records over the API), so any mail error is caught and logged.
        """
        template = self.env.ref(
            'futurekawa_quality.mail_template_quality_alert',
            raise_if_not_found=False)
        category = self.env.ref(
            'futurekawa_quality.partner_category_responsable_qualite',
            raise_if_not_found=False)
        if not template or not category:
            return

        recipients = self.env['res.partner'].search([
            ('category_id', 'in', category.ids),
            ('email', '!=', False),
        ])
        if not recipients:
            _logger.warning(
                "No 'Responsable Qualite FutureKawa' contact has an email; "
                "alert notification skipped. Tag a contact in the Contacts app.")
            return

        for record in self:
            try:
                template.send_mail(
                    record.id, force_send=True,
                    email_values={'recipient_ids': [(6, 0, recipients.ids)]})
                _logger.info("Alert email sent for %s (%s) to %d recipient(s)",
                             record.name, record.niveau, len(recipients))
            except Exception:
                _logger.exception("Failed to send the alert email for %s", record.name)

    def write(self, vals):
        """Stamp the resolution date whenever the ticket reaches a terminal state.

        The backend also writes `state` (one-way sync after a closure from the
        supervision frontend), so stamping here covers both entry points.
        """
        if vals.get('state') in ('resolved', 'rejected') and not vals.get('date_resolution'):
            vals = dict(vals, date_resolution=fields.Datetime.now())
        return super().write(vals)

    def action_investigate(self):
        self.write({'state': 'investigation'})
        self._push_state_to_backend()

    def action_approve(self):
        self.write({'state': 'resolved'})
        self._push_state_to_backend()

    def action_reject(self):
        self.write({'state': 'rejected'})
        self._push_state_to_backend()

    def _backend_base_url(self):
        """Base URL of the backend-local owning this ticket, from its country record."""
        self.ensure_one()
        pays = self.pays_id or self.env['futurekawa.pays'].sudo()._get_or_create(self.pays_code)
        if not pays:
            raise UserError(_(
                "This ticket has no country, so the alert cannot be sent back to a "
                "backend. Set the country on the ticket first."))
        if not pays.backend_url:
            raise UserError(_(
                "No backend URL set for country %s. Fill it in under "
                "FutureKawa Quality > Configuration > Pays."
            ) % pays.code)
        timeout = self.env['ir.config_parameter'].sudo().get_param(
            'futurekawa.backend_timeout_s', 5)
        return pays.backend_url.rstrip('/'), int(timeout)

    def _push_state_to_backend(self):
        """PATCH the alert status on the country backend that raised the ticket.

        The backend owns the alert lifecycle: as long as its alert stays OUVERTE it
        keeps deduplicating, so a ticket treated only in Odoo would silently mute
        every further alert for that entrepot. This is what closes that gap.

        A backend outage must never roll back the ERP decision, so failures are
        logged and posted in the chatter instead of raised.
        """
        for record in self:
            if not record.backend_alerte_id:
                continue
            statut = self._BACKEND_STATUT.get(record.state)
            if not statut:
                continue
            try:
                base_url, timeout = record._backend_base_url()
            except UserError as e:
                record.message_post(body=_("Alert status not sent to the backend: %s") % e)
                _logger.warning("Cannot resolve backend URL for %s: %s", record.name, e)
                continue

            url = "%s/api/v1/alertes/%s" % (base_url, record.backend_alerte_id)
            payload = json.dumps({'statutAlerte': statut}).encode()
            request = urllib.request.Request(
                url, data=payload, method='PATCH',
                headers={'Content-Type': 'application/json'})
            try:
                with urllib.request.urlopen(request, timeout=timeout) as response:
                    if response.status >= 300:
                        raise urllib.error.HTTPError(
                            url, response.status, response.reason, response.headers, None)
                _logger.info("Pushed state '%s' of %s to backend alert %s (%s)",
                             record.state, record.name, record.backend_alerte_id,
                             record.pays_code)
            except Exception as e:
                _logger.exception("Failed to push state of %s to the backend", record.name)
                record.message_post(body=_(
                    "Status '%(state)s' could NOT be sent to the %(pays)s backend "
                    "(%(error)s). The backend still considers this alert open, so new "
                    "alerts for this entrepot stay suppressed until it is retried."
                ) % {'state': record.state, 'pays': record.pays_code, 'error': e})
