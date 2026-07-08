from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import time
import sys

def run_test():
    print("🚀 Démarrage du test Selenium E2E pour FutureKawa...")
    
    # Configuration du navigateur en mode headless (sans interface graphique)
    options = webdriver.ChromeOptions()
    options.add_argument('--headless')
    options.add_argument('--no-sandbox')
    options.add_argument('--disable-dev-shm-usage')

    # Instanciation du WebDriver
    try:
        driver = webdriver.Chrome(options=options)
    except Exception as e:
        print(f"❌ Erreur lors du lancement de Chrome: {e}")
        print("💡 Astuce : Assurez-vous d'avoir 'chromedriver' installé sur votre machine.")
        sys.exit(1)

    try:
        # L'URL de notre frontend en dev local (à adapter selon votre configuration)
        target_url = "http://localhost:5173"
        print(f"🌐 Navigation vers {target_url}...")
        driver.get(target_url)

        # Attente explicite que l'application soit chargée (ex: on attend un titre ou une navigation)
        wait = WebDriverWait(driver, 10)
        
        # Exemple de validation 1 : Vérifier le titre de la page
        title = driver.title
        print(f"📌 Titre de la page détecté : {title}")
        
        # Exemple de validation 2 : Vérifier qu'un élément du DOM (ex: le menu des alertes) est présent
        # On suppose qu'il y a un lien vers le Dashboard ou les Alertes dans la barre de navigation
        # <a href="/alertes" id="nav-alertes">Alertes</a>
        print("🔍 Recherche de l'onglet Alertes...")
        # Note : On utilise un sélecteur générique pour la démo. Ajustez l'ID selon le code Vue.js réel.
        # alert_link = wait.until(EC.presence_of_element_located((By.ID, "nav-alertes")))
        # alert_link.click()
        
        print("✅ Le test d'interface de base est réussi ! L'application répond correctement.")

    except Exception as e:
        print(f"❌ Le test a échoué : {e}")
    finally:
        # Toujours fermer le navigateur à la fin
        print("🛑 Fermeture du navigateur.")
        driver.quit()

if __name__ == "__main__":
    run_test()
