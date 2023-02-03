import logging

from utils import *

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def present_measurement(binding: dict[str, str], historical: bool = False):
    if historical:
        print(
            f"[HISTORICAL] Temperature was {binding['temperature']} units at {binding['timestamp'][1:-1]}",
            flush=True,
        )
    else:
        print(
            f"[NEW!] Live temperature is {binding['temperature']} units at {binding['timestamp'][1:-1]}",
            flush=True,
        )


def handle_react_measurements(bindings):
    for binding in bindings:
        present_measurement(binding)
    return []


def start_ui_kb(kb_id, kb_name, kb_description, ke_endpoint):
    register_knowledge_base(kb_id, kb_name, kb_description, ke_endpoint)

    ask_measurements_ki = register_ask_knowledge_interaction(
        """
            ?sensor rdf:type saref:Sensor .
            ?measurement saref:measurementMadeBy ?sensor .
            ?measurement saref:isMeasuredIn saref:TemperatureUnit .
            ?measurement saref:hasValue ?temperature .
            ?measurement saref:hasTimestamp ?timestamp .
        """,
        "ask-measurements",
        kb_id,
        ke_endpoint,
        {
            "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
            "saref": "https://saref.etsi.org/core/",
        },
    )

    react_measurements_ki = register_react_knowledge_interaction(
        """
            ?sensor rdf:type saref:Sensor .
            ?measurement saref:measurementMadeBy ?sensor .
            ?measurement saref:isMeasuredIn saref:TemperatureUnit .
            ?measurement saref:hasValue ?temperature .
            ?measurement saref:hasTimestamp ?timestamp .
        """,
        None,
        "react-measurements",
        kb_id,
        ke_endpoint,
        {
            "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
            "saref": "https://saref.etsi.org/core/",
        },
    )

    historical_measurements = ask([{}], ask_measurements_ki, kb_id, ke_endpoint)
    for measurement in historical_measurements:
        present_measurement(measurement, historical=True)

    start_handle_loop(
        {
            react_measurements_ki: handle_react_measurements,
        },
        kb_id,
        ke_endpoint,
    )


if __name__ == "__main__":
    add_sigterm_hook()

    import time

    logger.info(
        "sleeping a bit, so that there are some historical measurements that we can demonstrate to show"
    )
    time.sleep(6)

    start_ui_kb(
        "http://example.org/ui",
        "UI",
        "UI for measurement",
        "http://knowledge-engine:8280/rest/",
    )
