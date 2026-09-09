"""Shared Selenium fixtures for the supervision UI recette.

The suite drives the frontend with `VITE_USE_MOCKS=true`, so the MSW fixtures in
`frontend-web/src/mocks` answer every API call. That makes the run deterministic and
independent of the country backends, which is what lets it execute in CI; the
integration between the tiers is covered by the Java suites instead.

Point it at an already-running server with E2E_BASE_URL, or let run-tests.sh start
one. Set E2E_HEADED=1 to watch the browser during a demo.
"""
import os
import time

import pytest
from selenium import webdriver
from selenium.common.exceptions import TimeoutException
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import Select, WebDriverWait

BASE_URL = os.environ.get("E2E_BASE_URL", "http://localhost:4173")
TIMEOUT = int(os.environ.get("E2E_TIMEOUT", "15"))

ANCHORS = (
    "lots-table",
    "alert-item",
    "kpi",
    "empty-state",
    "error-banner",
    "entrepot-nom",
    "login-form",
)

def pytest_configure(config):
    config.addinivalue_line("markers", "recette: functional acceptance scenario")

@pytest.fixture(scope="session")
def driver():
    options = webdriver.ChromeOptions()
    if not os.environ.get("E2E_HEADED"):
        options.add_argument("--headless=new")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--window-size=1440,900")
    try:
        instance = webdriver.Chrome(options=options)
    except Exception as exc:
        pytest.exit(
            f"Chrome could not be started ({exc}). Selenium Manager downloads the "
            "driver on first use, so this usually means Chrome or Chromium itself "
            "is missing.",
            returncode=3,
        )
    instance.set_page_load_timeout(TIMEOUT * 2)
    yield instance
    instance.quit()

class Supervision:
    """Page-object wrapper: one place to change when a selector moves."""

    def __init__(self, driver):
        self.driver = driver
        self.wait = WebDriverWait(driver, TIMEOUT)

    def _fingerprint(self):
        return self.driver.execute_script(
            "return document.querySelectorAll('[data-testid]').length"
            " + '/' + document.querySelectorAll('[data-testid=loading]').length"
            " + '/' + document.querySelectorAll('[data-testid=lot-row],"
            "[data-testid=alert-item],[data-testid=kpi]').length"
        )

    def settled(self):
        """Wait until an anchor is rendered and the DOM has stopped moving."""
        self.wait.until(
            lambda d: not d.find_elements(By.CSS_SELECTOR, "[data-testid=loading]")
            and any(d.find_elements(By.CSS_SELECTOR, f"[data-testid={a}]") for a in ANCHORS)
        )
        previous = None
        deadline = time.monotonic() + TIMEOUT
        while time.monotonic() < deadline:
            current = self._fingerprint()
            if current == previous:
                return self
            previous = current
            time.sleep(0.12)
        return self

    def wait_until(self, predicate, message=""):
        """Wait for a specific expectation, e.g. a status the user just changed."""
        self.wait.until(lambda _: predicate(), message)
        return self

    def wait_for(self, testid):
        return self.wait.until(
            EC.visibility_of_element_located((By.CSS_SELECTOR, f"[data-testid={testid}]"))
        )

    def wait_gone(self, testid):
        self.wait.until_not(
            EC.presence_of_element_located((By.CSS_SELECTOR, f"[data-testid={testid}]"))
        )
        return self

    def open(self, path="/"):
        self.driver.get(BASE_URL + path)
        return self

    def reset_session(self):
        self.driver.get(BASE_URL + "/login")
        self.driver.execute_script("window.localStorage.clear();")
        self.driver.get(BASE_URL + "/login")
        return self

    def login(self, identifiant="recette"):
        self.reset_session()
        field = self.wait_for("login-username")
        field.clear()
        field.send_keys(identifiant)
        self.one("login-submit").click()
        return self.settled()

    def goto(self, nav_path):
        self.driver.find_element(
            By.CSS_SELECTOR, f"[data-testid=nav-link][data-nav='{nav_path}']"
        ).click()
        return self.settled()

    def select_country(self, code):
        self.driver.find_element(
            By.CSS_SELECTOR, f"[data-testid=country-option][data-code='{code}']"
        ).click()
        return self.settled()

    def all(self, testid):
        return self.driver.find_elements(By.CSS_SELECTOR, f"[data-testid={testid}]")

    def one(self, testid):
        return self.driver.find_element(By.CSS_SELECTOR, f"[data-testid={testid}]")

    def exists(self, testid):
        return bool(self.all(testid))

    def text(self, testid):
        return self.one(testid).text.strip()

    def kpi(self, label):
        element = self.driver.find_element(
            By.CSS_SELECTOR, f"[data-testid=kpi][data-label='{label}']"
        )
        return int(element.find_element(By.CSS_SELECTOR, "[data-testid=kpi-value]").text)

    def lot_rows(self):
        return [
            {
                "reference": row.get_attribute("data-reference"),
                "pays": row.get_attribute("data-pays"),
                "statut": row.get_attribute("data-statut"),
            }
            for row in self.all("lot-row")
        ]

    def alert_items(self):
        return [
            {
                "pays": item.get_attribute("data-pays"),
                "niveau": item.get_attribute("data-niveau"),
                "statut": item.get_attribute("data-statut"),
                "type": item.get_attribute("data-type"),
            }
            for item in self.all("alert-item")
        ]

    def has_canvas(self):
        return bool(self.driver.find_elements(By.CSS_SELECTOR, "canvas"))

    def current_path(self):
        return self.driver.current_url[len(BASE_URL):]

    def click(self, testid):
        self.one(testid).click()
        return self

    def choose(self, testid, value):
        Select(self.one(testid)).select_by_value(value)
        return self.settled()

    def fill(self, testid, value):
        field = self.one(testid)
        field.clear()
        field.send_keys(value)
        return self

@pytest.fixture
def app(driver):
    """A logged-in supervision poste, on a clean session for every test."""
    page = Supervision(driver)
    try:
        page.login()
    except TimeoutException:
        pytest.fail(
            f"The frontend did not answer at {BASE_URL}. Start it with "
            "tests-e2e/run-tests.sh, or set E2E_BASE_URL."
        )
    return page
