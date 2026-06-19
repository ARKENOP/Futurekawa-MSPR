{
    'name': 'FutureKawa Quality Alerts',
    'version': '18.0.1.0.0',
    'summary': 'Automated quality non-conformity alerts from local IoT sensors',
    'description': """
FutureKawa Quality Alerts
=========================
Receives storage anomalies (temperature / humidity drift, expired lots) pushed
by the local Spring Boot backends over the Odoo external API (JSON-RPC) and
records them as auditable Quality Alert tickets visible in the Odoo interface.
""",
    'category': 'Quality',
    'author': 'FutureKawa IT',
    'depends': ['base', 'mail'],
    'data': [
        'security/ir.model.access.csv',
        'data/ir_sequence_data.xml',
        'data/mail_template_data.xml',
        'views/quality_alert_views.xml',
    ],
    'installable': True,
    'application': True,
    'license': 'LGPL-3',
}
