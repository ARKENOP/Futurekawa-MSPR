import logging

from odoo import models, fields, api

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

    # ── Origin (pushed by the local backend) ────────────────────────────────
    backend_alerte_id = fields.Integer(
        string='ID Alerte (Backend)', index=True, copy=False,
        help="Identifiant de l'alerte dans la base du backend local. "
             "Sert de cle d'idempotence pour eviter les doublons.",
    )
    entrepot_nom = fields.Char(string='Entrepot', required=True, tracking=True)
    pays_code = fields.Selection(
        selection=[('BR', 'Bresil'), ('EC', 'Equateur'), ('CO', 'Colombie')],
        string='Pays', tracking=True,
    )
    lot_reference = fields.Char(string='Reference Lot', tracking=True)

    # ── Classification (mirrors the backend enums) ──────────────────────────
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

    # ── Measured values (optional) ──────────────────────────────────────────
    valeur_enregistree = fields.Float(string='Valeur Enregistree')
    valeur_cible = fields.Float(string='Valeur Cible (Seuil)')
    message_description = fields.Text(string='Description')

    # ── Lifecycle ───────────────────────────────────────────────────────────
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

    @api.model_create_multi
    def create(self, vals_list):
        for vals in vals_list:
            if vals.get('name', 'New') == 'New':
                vals['name'] = self.env['ir.sequence'].next_by_code(
                    'futurekawa.quality.alert') or 'ALT/'
        records = super().create(vals_list)
        records._notify_quality_team()
        return records

    def _notify_quality_team(self):
        """Email the quality team for CRITICAL alerts.

        Recipients are resolved dynamically from the Contacts tagged
        "Responsable Qualite FutureKawa" (no hardcoded addresses). Failures must
        never block ticket creation (the backend creates these records over the
        API), so any mail error is caught and logged.
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
                "critical-alert email skipped.")
            return

        for record in self:
            if record.niveau != 'critique':
                continue
            try:
                template.send_mail(
                    record.id, force_send=True,
                    email_values={'recipient_ids': [(6, 0, recipients.ids)]})
                _logger.info("Quality alert email sent for %s to %d recipient(s)",
                             record.name, len(recipients))
            except Exception:
                _logger.exception(
                    "Failed to send quality alert email for %s", record.name)

    def action_investigate(self):
        self.write({'state': 'investigation'})

    def action_approve(self):
        self.write({'state': 'resolved', 'date_resolution': fields.Datetime.now()})

    def action_reject(self):
        self.write({'state': 'rejected', 'date_resolution': fields.Datetime.now()})
