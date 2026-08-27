# Odoo 18 Addons — Implementation Plan

This implementation plan details the custom ERP development requirements to support the FutureKawa MSPR coffee tracking application.

Following the **RNCP Bloc 4 certification** requirements for custom ERP programming, we define two custom Odoo v18 Community modules (`futurekawa_quality` and `futurekawa_inventory`). Odoo's role is strictly ERP: non-conformity tickets, the FIFO recommendation engine, and the e-mail notification of the quality team.

## Status (27/08/2026)

| Module | State |
| --- | --- |
| `futurekawa_quality` | **Implemented and installed** on the Odoo 18 instance. Receives tickets from the country backends over JSON-RPC and e-mails the quality team on critical alerts. The code as shipped differs from §3 below — see "As-built" right after it. |
| `futurekawa_inventory` | **Not implemented.** §4 remains the specification to build. |
| SSO / OAuth | **Dropped.** Odoo uses its own local accounts. There is no Keycloak in the solution: user authentication is out of scope for this phase (see `docs/ARCHITECTURE.md` §6), so the `auth_oauth` configuration described in earlier revisions of this plan no longer applies.

---

## 1. Directory Structure

Custom addons will be placed in the `/odoo/addons/` directory:

```text
odoo/
├── addons/
│   ├── futurekawa_quality/
│   │   ├── __init__.py
│   │   ├── __manifest__.py
│   │   ├── security/
│   │   │   └── ir.model.access.csv
│   │   ├── models/
│   │   │   ├── __init__.py
│   │   │   └── quality_alert.py
│   │   └── views/
│   │       └── quality_alert_views.xml
│   └── futurekawa_inventory/
│       ├── __init__.py
│       ├── __manifest__.py
│       ├── security/
│       │   └── ir.model.access.csv
│       ├── models/
│       │   ├── __init__.py
│       │   └── stock_lot.py
│       └── views/
│           └── stock_lot_views.xml
└── implementation_plan.md (this file)
```

---

## 2. Authentication

Odoo authenticates its own users locally. **No external identity provider.** An earlier
revision of this plan configured Keycloak as an OAuth2 provider through the built-in
`auth_oauth` module; that was dropped along with Keycloak itself.

The backends do not log in as users: each country backend authenticates against the JSON-RPC
API with `common.authenticate(db, login, apiKey)`, using an Odoo **API key** generated from
the admin's *My Profile → Account Security* (requires Developer Mode). The key lives only in
the backend's `.env`, never in source.

## 3. Module 1: `futurekawa_quality` (Automated Non-Conformities)

This module handles anomalies received from local backends (via XML-RPC or JSON-RPC API calls) and records them as formal, auditable Quality Alert tickets inside the ERP.

### 3.1 Manifest: `futurekawa_quality/__manifest__.py`
```python
{
    'name': 'FutureKawa Quality Alerts',
    'version': '18.0.1.0.0',
    'summary': 'Automated quality non-conformity alerts from local IoT sensors',
    'category': 'Quality',
    'author': 'FutureKawa IT',
    'depends': ['base', 'mail'],
    'data': [
        'security/ir.model.access.csv',
        'views/quality_alert_views.xml',
    ],
    'installable': True,
    'application': True,
}
```

### 3.2 Python Model: `futurekawa_quality/models/quality_alert.py`
```python
from odoo import models, fields, api

class QualityAlert(models.Model):
    _name = 'futurekawa.quality.alert'
    _description = 'Fiche de Non-Conformité Qualité FutureKawa'
    _inherit = ['mail.thread', 'mail.activity.mixin']
    _order = 'date_creation desc'

    name = fields.Char(string='Référence Alerte', required=True, copy=False, readonly=True, index=True, default=lambda self: 'New')
    lot_reference = fields.Char(string='Référence Lot', required=True, tracking=True)
    entrepot_nom = fields.Char(string='Entrepôt', required=True)
    pays_code = fields.Selection([
        ('BR', 'Brésil'),
        ('EC', 'Équateur'),
        ('CO', 'Colombie')
    ], string='Pays', required=True, tracking=True)
    
    type_anomaly = fields.Selection([
        ('temperature', 'Température Hors-Plage'),
        ('humidity', 'Humidité Hors-Plage'),
        ('both', 'Conditions Globales Critiques')
    ], string='Type Anomalie', required=True)
    
    valeur_enregistree = fields.Float(string='Valeur Enregistrée')
    valeur_cible = fields.Float(string='Valeur Cible (Seuil)')
    date_creation = fields.Datetime(string='Date de Détection', default=fields.Datetime.now, readonly=True)
    date_resolution = fields.Datetime(string='Date de Résolution', tracking=True)
    
    state = fields.Selection([
        ('draft', 'Détecté (Brouillon)'),
        ('investigation', 'En cours d\'analyse'),
        ('resolved', 'Conforme (Résolu)'),
        ('rejected', 'Lot Déclassé / Perdu')
    ], string='Statut', default='draft', required=True, tracking=True)
    
    notes_audit = fields.Text(string='Notes d\'Audit Qualité')
    responsable_id = fields.Many2one('res.users', string='Responsable Qualité', tracking=True)

    @api.model_create_multi
    def create(self, vals_list):
        for vals in vals_list:
            if vals.get('name', 'New') == 'New':
                vals['name'] = self.env['ir.sequence'].next_by_code('futurekawa.quality.alert') or 'ALT/'
        return super(QualityAlert, self).create(vals_list)

    def action_approve(self):
        self.write({'state': 'resolved', 'date_resolution': fields.Datetime.now()})

    def action_reject(self):
        self.write({'state': 'rejected', 'date_resolution': fields.Datetime.now()})
```

### 3.3 Views: `futurekawa_quality/views/quality_alert_views.xml`
```xml
<odoo>
    <!-- Form View -->
    <record id="view_quality_alert_form" model="ir.ui.view">
        <name>futurekawa.quality.alert.form</name>
        <model>futurekawa.quality.alert</model>
        <arch type="xml">
            <form string="Alerte Qualité">
                <header>
                    <button name="action_approve" type="object" string="Valider / Résoudre" class="oe_highlight" invisible="state in ('resolved', 'rejected')"/>
                    <button name="action_reject" type="object" string="Déclasser le Lot" class="btn-danger" invisible="state in ('resolved', 'rejected')"/>
                    <field name="state" widget="statusbar" statusbar_visible="draft,investigation,resolved,rejected"/>
                </header>
                <sheet>
                    <div class="oe_title">
                        <h1>
                            <field name="name" readonly="1"/>
                        </h1>
                    </div>
                    <group>
                        <group string="Informations Capteur">
                            <field name="lot_reference"/>
                            <field name="entrepot_nom"/>
                            <field name="pays_code"/>
                            <field name="date_creation"/>
                        </group>
                        <group string="Données Métrologiques">
                            <field name="type_anomaly"/>
                            <field name="valeur_enregistree"/>
                            <field name="valeur_cible"/>
                            <field name="responsable_id"/>
                        </group>
                    </group>
                    <notebook>
                        <page string="Notes d'Audit">
                            <field name="notes_audit" placeholder="Décrivez l'origine de l'anomalie et les actions correctives prises..."/>
                        </page>
                    </notebook>
                </sheet>
                <chatter/>
            </form>
        </arch>
    </record>

    <!-- Tree View -->
    <record id="view_quality_alert_tree" model="ir.ui.view">
        <name>futurekawa.quality.alert.tree</name>
        <model>futurekawa.quality.alert</model>
        <arch type="xml">
            <list string="Alertes Qualité" decoration-danger="state == 'draft'" decoration-warning="state == 'investigation'" decoration-success="state == 'resolved'">
                <field name="name"/>
                <field name="lot_reference"/>
                <field name="pays_code"/>
                <field name="type_anomaly"/>
                <field name="valeur_enregistree"/>
                <field name="date_creation"/>
                <field name="state" widget="badge"/>
            </list>
        </arch>
    </record>

    <!-- Window Action -->
    <record id="action_quality_alerts" model="ir.actions.act_window">
        <name>Fiches de Non-Conformité</name>
        <res_model>futurekawa.quality.alert</res_model>
        <view_mode>list,form</view_mode>
    </record>

    <!-- Menu Items -->
    <menuitem id="menu_futurekawa_root" name="FutureKawa Quality" web_icon="futurekawa_quality,static/description/icon.png"/>
    <menuitem id="menu_quality_alerts" name="Fiches d'Alertes" parent="menu_futurekawa_root" action="action_quality_alerts"/>
</odoo>
```

### 3.4 Security: `futurekawa_quality/security/ir.model.access.csv`
```csv
id,name,model_id:id,group_id:id,perm_read,perm_write,perm_create,perm_unlink
access_quality_alert_user,access_quality_alert_user,model_futurekawa_quality_alert,base.group_user,1,1,1,0
```

---

### 3.5 As-built (what is actually installed)

The shipped module in `odoo/addons/futurekawa_quality/` evolved from the specification above:

- **Model fields** follow the backend's alert model rather than a metrology reading:
  `name` (sequence `ALT/%(year)s/`), `entrepot_nom`, `pays_code`, `lot_reference`,
  `type_anomaly` (`condition_non_ideale` / `lot_trop_ancien`, mirroring `TypeAlerte`),
  `niveau` (`info` / `warning` / `critique`, mirroring `NiveauAlerte`),
  `message_description`, `state`, and `backend_alerte_id` — the routing key the backend uses
  to find the ticket again when the alert is closed.
- **State mapping** with the backend, in both directions:
  outbound `OUVERTE → draft`, `NOTIFIEE → investigation`, `CLOTUREE → resolved`;
  inbound `draft → OUVERTE`, `investigation → NOTIFIEE`, `resolved → CLOTUREE`,
  `rejected → CLOTUREE`. The backend has three states only, so declassing also closes the
  alert and the "declassed" nuance stays in the ERP's own state and audit trail.
- **The buttons call back into the owning backend** (added 27/08/2026).
  `_push_state_to_backend()` sends `PATCH {backend}/api/v1/alertes/{backend_alerte_id}`
  with `{"statutAlerte": ...}`; the base URL comes from `ir.config_parameter`
  (`futurekawa.backend_url.<PAYS>`, seeded by `data/backend_config_data.xml`, timeout
  `futurekawa.backend_timeout_s`). The backend stays the source of truth for the alert
  lifecycle, and this is what keeps its deduplication honest: while its alert is `OUVERTE`
  it suppresses new ones, so a ticket treated only in Odoo used to mute that entrepot
  permanently.
  A backend outage never rolls back the ERP decision — the failure is logged and posted in
  the ticket's chatter, telling the user the backend still considers the alert open.
  `create()` deliberately does **not** call back: the ticket came from the backend. The
  return write from the backend is idempotent, so the loop terminates.
- **Countries are data, not code** (added 27/08/2026). Model `futurekawa.pays`
  (`code` unique, `nom`, `backend_url`, `actif`) with an editable list and a form under
  *FutureKawa Quality → Configuration → Pays*. `pays_code` on the alert is a `Char`, not a
  `Selection`: a fixed list would have **rejected any alert from a country added later**.
  How a country gets into Odoo:
  1. **Automatically, on its first alert.** `create()` resolves `pays_code` through
     `futurekawa.pays._get_or_create()`, which creates the record if the code is unknown.
     The backend also sends `pays_nom` (popped in `create()`, not a field on the ticket) so
     the new country is named properly instead of showing its bare code.
  2. **Manually**, through the same menu, to register a country before it ever alerts.
  In both cases an administrator must fill in **`backend_url`** for that country before its
  quality decisions can be pushed back — rows missing it are highlighted in the list. Until
  then, clicking a decision button still updates the ERP and posts a message in the ticket's
  chatter explaining that the backend was not notified.
  Odoo has **no connection to `backend-central`**: it receives tickets from each country
  backend and answers to that same backend. Routing both through the central (one URL in
  Odoo, plus active country discovery via `GET {central}/api/v1/pays`) was considered and
  deliberately not chosen, to avoid making the central a dependency of the ERP→backend path.
- **Duplicate guard**: an `@api.constrains` refuses a second ticket for the same
  `(backend_alerte_id, pays_code)`. Not a SQL `UNIQUE`, because Odoo stores an unset Integer
  as `0` and hand-created tickets would all collide on `(0, country)`.
- **`date_resolution`** is stamped by a `write()` override rather than by the buttons, so it
  is filled whichever side closed the alert (ERP button, or the backend after a closure from
  the supervision frontend).
- **`valeur_enregistree` / `valeur_cible` were removed from the form view**: the backend
  never populates them, so they always displayed 0.00. The measured values are in the
  description text.
- **E-mail** is Odoo's responsibility, not the backend's: `data/mail_template_data.xml`
  defines the `mail.template`, and `create()` fires it **only for `niveau == 'critique'`,
  wrapped in a try/except so a mail failure never blocks ticket creation.
- **Recipients are not hardcoded.** The module ships a `res.partner.category` tag
  "Responsable Qualite FutureKawa"; `_notify_quality_team()` resolves every partner carrying
  that tag and an e-mail address, and passes them as `recipient_ids`. Managing the
  distribution list means tagging a contact in the Contacts app — no code change.
- **Installation note**: install with a one-shot CLI run
  (`docker run ... odoo -i futurekawa_quality --stop-after-init`). Installing through
  `docker exec` against the live server deadlocks on foreign-key creation.

---

## 4. Module 2: `futurekawa_inventory` (FIFO Recommendation Engine)

This module inherits Odoo's native stock lots, updates compliance conditions synced from sensor data, and provides automated FIFO selection algorithms to enforce the rule of shipping the oldest compliant stock.

### 4.1 Manifest: `futurekawa_inventory/__manifest__.py`
```python
{
    'name': 'FutureKawa FIFO Inventory',
    'version': '18.0.1.0.0',
    'summary': 'FIFO Coffee Lot Recommendations and Storage History Tracking',
    'category': 'Inventory',
    'author': 'FutureKawa IT',
    'depends': ['stock'],
    'data': [
        'views/stock_lot_views.xml',
    ],
    'installable': True,
}
```

### 4.2 Python Model: `futurekawa_inventory/models/stock_lot.py`
```python
from odoo import models, fields, api

class StockLot(models.Model):
    _inherit = 'stock.lot'

    date_entree_stockage = fields.Datetime(string="Date d'Entrée en Stockage", required=True, default=fields.Datetime.now)
    date_recolte = fields.Date(string="Date de Récolte")
    
    statut_lot = fields.Selection([
        ('conforme', 'Conforme'),
        ('en_alerte', 'En Alerte'),
        ('perime', 'Périmé')
    ], string="État de Conservation", default='conforme', required=True, tracking=True)

    @api.model
    def recommend_fifo_lot(self, product_id, quantity, location_id):
        """
        Algorithm returning the oldest compliant lots (date_entree_stockage ASC)
        located in the active warehouse.
        """
        quants = self.env['stock.quant'].search([
            ('product_id', '=', product_id),
            ('location_id', '=', location_id),
            ('quantity', '>', 0),
            ('lot_id.statut_lot', '=', 'conforme')
        ])
        
        # Sort lots by the stockage arrival date (FIFO)
        sorted_quants = quants.sorted(key=lambda q: q.lot_id.date_entree_stockage or fields.Datetime.now())
        
        selected_lots = []
        accumulated_qty = 0.0
        
        for quant in sorted_quants:
            if accumulated_qty >= quantity:
                break
            needed = quantity - accumulated_qty
            allocated = min(quant.quantity, needed)
            selected_lots.append({
                'lot_id': quant.lot_id.id,
                'lot_name': quant.lot_id.name,
                'quantity': allocated,
                'date_entree': quant.lot_id.date_entree_stockage
            })
            accumulated_qty += allocated
            
        return selected_lots
```

### 4.3 Views: `futurekawa_inventory/views/stock_lot_views.xml`
```xml
<odoo>
    <!-- Inherit Odoo Stock Lot Form View to show storage specifics -->
    <record id="view_production_lot_form_inherit" model="ir.ui.view">
        <name>stock.lot.form.inherit</name>
        <model>stock.lot</model>
        <inherit_id ref="stock.view_production_lot_form"/>
        <arch type="xml">
            <xpath expr="//group[@name='main_group']" position="after">
                <group string="Caractéristiques FutureKawa MSPR">
                    <group>
                        <field name="date_entree_stockage"/>
                        <field name="date_recolte"/>
                    </group>
                    <group>
                        <field name="statut_lot" widget="radio" class="oe_inline"/>
                    </group>
                </group>
            </xpath>
        </arch>
    </record>

    <!-- Inherit Odoo Stock Lot Tree/List View to show conservation state -->
    <record id="view_production_lot_tree_inherit" model="ir.ui.view">
        <name>stock.lot.list.inherit</name>
        <model>stock.lot</model>
        <inherit_id ref="stock.view_production_lot_tree"/>
        <arch type="xml">
            <xpath expr="//field[@name='create_date']" position="after">
                <field name="date_entree_stockage"/>
                <field name="statut_lot" widget="badge" 
                       decoration-success="statut_lot == 'conforme'" 
                       decoration-warning="statut_lot == 'en_alerte'" 
                       decoration-danger="statut_lot == 'perime'"/>
            </xpath>
        </arch>
    </record>
</odoo>
```

---

## 5. Summary of Verification Plan

1. **Local Alert Generation (E2E Test)**:
   * Execute an API XML-RPC Python call on `futurekawa.quality.alert` using custom scripts mimicking `backend-local`.
   * Verify that a new ticket `ALT/...` is created in Odoo and appears in the Odoo Quality Kanban view with state `draft`.
   
2. **FIFO Recommendation (Unit/Integration Test)**:
   * Set up multiple lots of `product.product` with different values of `date_entree_stockage` (e.g. Lot A: 6 months ago, Lot B: 1 month ago, Lot C: 12 months ago - but marked `perime`).
   * Trigger `recommend_fifo_lot()`.
   * Validate that the function returns **Lot A** first, completely ignoring the expired **Lot C** despite it being the oldest.
