import logging

from odoo import models, fields, api, _
from odoo.exceptions import ValidationError

_logger = logging.getLogger(__name__)

class FuturekawaPays(models.Model):
    """A country served by its own backend-local instance.

    Countries are data, not code: the solution deploys one backend-local per country
    and each one declares its own identity through its .env (COUNTRY_CODE /
    COUNTRY_NAME). Adding a country therefore means deploying a backend and creating
    a record here — never editing this module.

    The record also carries the base URL used to push the quality team's decisions
    back to that country's backend (see quality_alert._push_state_to_backend).
    """

    _name = 'futurekawa.pays'
    _description = 'Pays FutureKawa'
    _order = 'code'
    _rec_name = 'code'

    code = fields.Char(
        string='Code Pays', required=True, index=True,
        help="Code envoye par le backend du pays (COUNTRY_CODE), ex. BR.",
    )
    nom = fields.Char(string='Nom du Pays')
    backend_url = fields.Char(
        string='URL du Backend',
        help="Base URL du backend-local de ce pays, ex. http://backend-local-br:8081. "
             "Laisser vide desactive le retour des decisions qualite vers ce pays.",
    )
    actif = fields.Boolean(string='Actif', default=True)
    alerte_ids = fields.One2many('futurekawa.quality.alert', 'pays_id', string='Alertes')
    alerte_count = fields.Integer(string="Nombre d'alertes", compute='_compute_alerte_count')

    _sql_constraints = [
        ('code_uniq', 'UNIQUE(code)', 'This country code already exists.'),
    ]

    @api.depends('alerte_ids')
    def _compute_alerte_count(self):
        for record in self:
            record.alerte_count = len(record.alerte_ids)

    @api.constrains('backend_url')
    def _check_backend_url(self):
        for record in self:
            if record.backend_url and not record.backend_url.startswith(('http://', 'https://')):
                raise ValidationError(_("The backend URL must start with http:// or https://"))

    @api.depends('code', 'nom')
    def _compute_display_name(self):
        for record in self:
            record.display_name = '%s - %s' % (record.code, record.nom) if record.nom else record.code

    @api.model
    def _get_or_create(self, code, nom=None):
        """Country matching `code`, created on the fly if the code is unknown.

        A backend pushing an alert must never fail because its country has not been
        registered yet: the record is created with the code alone, and an administrator
        completes its name and backend URL afterwards.
        """
        if not code:
            return self.browse()
        pays = self.search([('code', '=', code)], limit=1)
        if pays:
            if nom and not pays.nom:
                pays.nom = nom
            return pays
        _logger.info("Unknown country '%s' pushed by a backend; creating its record.", code)
        return self.create({'code': code, 'nom': nom or code})
