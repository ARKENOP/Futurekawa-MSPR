"""Recette fonctionnelle de l'interface de supervision (siège).

Each test is one scenario from docs/plan-de-tests.md §2, driven through the browser
exactly as an operator would: the country scope, the FIFO lot list, the alert
lifecycle, and the storage-condition charts. The expected values come from the MSW
fixtures in frontend-web/src/mocks/fixtures/db.ts.
"""
import pytest
from selenium.webdriver.common.by import By

pytestmark = pytest.mark.recette

class TestAccesEtPerimetre:
    def test_un_visiteur_non_identifie_est_renvoye_vers_le_login(self, driver):
        from conftest import Supervision

        page = Supervision(driver).reset_session()
        page.open("/lots")

        assert page.wait_for("login-form") is not None
        assert page.current_path().startswith("/login")

    def test_identification_du_poste_puis_acces_au_tableau_de_bord(self, driver):
        from conftest import Supervision

        page = Supervision(driver).login("qualiticien")

        assert page.current_path() in ("/", "")
        assert page.text("current-user") == "qualiticien"
        assert page.text("page-title") == "Tableau de bord"

    def test_le_perimetre_par_defaut_couvre_tous_les_pays(self, app):
        app.goto("/lots")

        pays = {row["pays"] for row in app.lot_rows()}
        assert pays == {"BR", "EC", "CO"}

    def test_choisir_un_pays_restreint_la_consultation(self, app):
        app.goto("/lots").select_country("BR")

        pays = {row["pays"] for row in app.lot_rows()}
        assert pays == {"BR"}, f"Le périmètre BR laisse passer {pays}"

    def test_revenir_a_tous_les_pays_reaffiche_la_consolidation(self, app):
        app.goto("/lots").select_country("CO")
        assert {r["pays"] for r in app.lot_rows()} == {"CO"}

        app.select_country("ALL")

        assert {r["pays"] for r in app.lot_rows()} == {"BR", "EC", "CO"}

    def test_la_deconnexion_ramene_au_login(self, app):
        app.click("logout")

        assert app.wait_for("login-form") is not None

class TestTableauDeBord:
    def test_les_indicateurs_comptent_les_lots_par_statut(self, app):
        assert app.kpi("Lots conformes") == 4
        assert app.kpi("Lots en alerte") == 1
        assert app.kpi("Lots périmés") == 1

    def test_les_alertes_ouvertes_sont_comptees_hors_alertes_cloturees(self, app):
        assert app.kpi("Alertes ouvertes") == 3

    def test_les_indicateurs_suivent_le_perimetre_pays(self, app):
        app.select_country("CO")

        assert app.kpi("Lots conformes") == 2
        assert app.kpi("Lots périmés") == 0
        assert app.kpi("Alertes ouvertes") == 0

    def test_chaque_entrepot_du_perimetre_est_represente(self, app):
        app.select_country("BR")

        entrepots = {card.get_attribute("data-entrepot") for card in app.all("gauge-card")}
        assert entrepots == {"BR-1", "BR-2"}

    def test_la_courbe_des_conditions_est_tracee(self, app):
        app.settled()

        assert app.has_canvas(), (
            "Aucun canvas ECharts : la courbe température/humidité n'est pas rendue"
        )

    def test_un_entrepot_ouvre_sa_fiche_detaillee(self, app):
        app.select_country("BR")
        app.driver.find_element(
            By.CSS_SELECTOR, "[data-testid=gauge-card][data-entrepot='BR-1']"
        ).click()
        app.settled()

        assert app.current_path().startswith("/entrepots/BR/1")
        assert app.text("entrepot-nom") == "Entrepôt Norte"

class TestLotsEtRotationFifo:
    def test_les_lots_sont_listes_du_plus_ancien_au_plus_recent(self, app):
        """FIFO : le lot le plus ancien doit sortir en premier."""
        app.goto("/lots").select_country("BR")

        references = [row["reference"] for row in app.lot_rows()]
        assert references[0] == "BR-2025-0188", (
            f"Le plus ancien lot BR devrait être en tête, obtenu {references}"
        )

    def test_le_filtre_de_statut_ne_laisse_que_les_lots_demandes(self, app):
        app.goto("/lots").choose("lot-statut-filter", "PERIME")

        statuts = {row["statut"] for row in app.lot_rows()}
        assert statuts == {"PERIME"}

    def test_le_filtre_de_statut_se_combine_avec_le_perimetre_pays(self, app):
        app.goto("/lots").select_country("BR").choose("lot-statut-filter", "CONFORME")

        rows = app.lot_rows()
        assert rows, "Le Brésil a un lot conforme dans les jeux d'essai"
        assert all(r["pays"] == "BR" and r["statut"] == "CONFORME" for r in rows)

    def test_un_perimetre_sans_lot_affiche_un_etat_vide_explicite(self, app):
        app.goto("/lots").select_country("EC").choose("lot-statut-filter", "PERIME")

        assert app.exists("empty-state")
        assert not app.lot_rows()

    def test_creer_un_lot_le_fait_apparaitre_dans_la_liste(self, app):
        app.goto("/lots").select_country("BR")
        avant = {row["reference"] for row in app.lot_rows()}

        app.click("lot-create-open")
        app.wait_for("lot-form-reference")
        app.choose("lot-form-pays", "BR")
        app.fill("lot-form-reference", "BR-RECETTE-001")
        app.click("lot-create-submit")
        app.wait_gone("lot-form-reference")
        app.wait_until(
            lambda: "BR-RECETTE-001" in {r["reference"] for r in app.lot_rows()},
            "Le lot créé n'est jamais apparu dans la liste",
        )

        apres = {row["reference"] for row in app.lot_rows()}
        assert "BR-RECETTE-001" in apres - avant

    def test_un_lot_cree_est_conforme_et_rattache_a_son_pays(self, app):
        app.goto("/lots").select_country("CO")

        app.click("lot-create-open")
        app.wait_for("lot-form-reference")
        app.choose("lot-form-pays", "CO")
        app.fill("lot-form-reference", "CO-RECETTE-002")
        app.click("lot-create-submit")
        app.wait_gone("lot-form-reference")
        app.wait_until(
            lambda: "CO-RECETTE-002" in {r["reference"] for r in app.lot_rows()},
            "Le lot créé n'est jamais apparu dans la liste",
        )

        cree = [r for r in app.lot_rows() if r["reference"] == "CO-RECETTE-002"]
        assert cree, "Le lot créé pour la Colombie n'apparaît pas dans son périmètre"
        assert cree[0] == {"reference": "CO-RECETTE-002", "pays": "CO", "statut": "CONFORME"}

    def test_annuler_la_creation_ne_cree_rien(self, app):
        app.goto("/lots").select_country("BR")
        avant = app.lot_rows()

        app.click("lot-create-open")
        app.wait_for("lot-form-reference")
        app.fill("lot-form-reference", "BR-JAMAIS-CREE")
        app.click("lot-create-cancel")
        app.wait_gone("lot-form-reference")

        assert app.lot_rows() == avant

    def test_changer_le_statut_dun_lot_met_la_pastille_a_jour(self, app):
        app.goto("/lots").select_country("EC")
        ligne = app.all("lot-row")[0]
        avant = ligne.get_attribute("data-statut")

        ligne.find_element(By.CSS_SELECTOR, "[data-testid=lot-cycle-statut]").click()
        app.wait_until(
            lambda: app.all("lot-row")[0].get_attribute("data-statut") != avant,
            "Le statut du lot n'a pas changé",
        )

        apres = app.all("lot-row")[0].get_attribute("data-statut")
        assert apres != avant
        assert apres in ("CONFORME", "EN_ALERTE", "PERIME")

class TestAlertes:
    def test_les_alertes_sont_listees_de_la_plus_recente_a_la_plus_ancienne(self, app):
        app.goto("/alertes")

        horodatages = [item.text for item in app.driver.find_elements(By.CSS_SELECTOR, ".when")]
        assert horodatages == sorted(horodatages, reverse=True)

    def test_le_filtre_de_statut_ne_laisse_que_les_alertes_demandees(self, app):
        app.goto("/alertes").choose("alerte-statut-filter", "CLOTUREE")

        statuts = {item["statut"] for item in app.alert_items()}
        assert statuts == {"CLOTUREE"}

    def test_le_filtre_de_type_isole_les_lots_trop_anciens(self, app):
        app.goto("/alertes").choose("alerte-type-filter", "LOT_TROP_ANCIEN")

        types = {item["type"] for item in app.alert_items()}
        assert types == {"LOT_TROP_ANCIEN"}

    def test_le_filtre_notifiee_est_selectionnable(self, app):
        """NOTIFIEE n'est produit que par le retour Odoo : l'option doit rester offerte."""
        app.goto("/alertes").choose("alerte-statut-filter", "NOTIFIEE")

        assert not app.alert_items()
        assert app.exists("empty-state")

    def test_les_alertes_suivent_le_perimetre_pays(self, app):
        app.goto("/alertes").select_country("EC")

        pays = {item["pays"] for item in app.alert_items()}
        assert pays == {"EC"}

    def test_cloturer_une_alerte_change_son_statut_et_retire_le_bouton(self, app):
        app.goto("/alertes").select_country("EC")
        assert app.alert_items()[0]["statut"] == "OUVERTE"

        app.click("alert-close")
        app.wait_until(
            lambda: app.alert_items()[0]["statut"] == "CLOTUREE",
            "L'alerte n'est pas passée à CLOTUREE",
        )

        assert app.alert_items()[0]["statut"] == "CLOTUREE"
        assert not app.exists("alert-close"), (
            "Une alerte clôturée ne doit plus proposer la clôture"
        )

    def test_une_alerte_critique_est_signalee_comme_telle(self, app):
        app.goto("/alertes").select_country("BR").choose(
            "alerte-type-filter", "CONDITION_NON_IDEALE"
        )

        niveaux = {item["niveau"] for item in app.alert_items()}
        assert "CRITIQUE" in niveaux

class TestFicheEntrepot:
    def test_la_fiche_affiche_les_deux_jauges_de_conditions(self, app):
        app.open("/entrepots/BR/1").settled()

        jauges = app.one("entrepot-gauges").text.lower()
        assert "température" in jauges
        assert "humidité" in jauges
        assert "29" in jauges and "55" in jauges, (
            f"Les seuils du Brésil doivent être affichés, obtenu : {jauges!r}"
        )

    def test_la_fiche_trace_lhistorique_des_conditions(self, app):
        app.open("/entrepots/BR/1").settled()

        assert app.has_canvas()

    def test_la_fiche_liste_les_lots_stockes_dans_lentrepot(self, app):
        app.open("/entrepots/BR/1").settled()

        references = {lot.get_attribute("data-reference") for lot in app.all("entrepot-lot")}
        assert "BR-2026-0001" in references

    def test_un_entrepot_inconnu_est_signale_sans_bloquer_linterface(self, app):
        app.open("/entrepots/BR/999").settled()

        assert app.exists("empty-state"), "L'opérateur doit lire « Entrepôt introuvable »"
        assert not app.exists("loading"), "L'interface ne doit pas rester sur le spinner"
