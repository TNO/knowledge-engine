import logging

from utils import *

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

measurements = []


def handle_answer_measurements(query_bindings):
    global measurements
    return match_bindings(query_bindings, measurements)


def handle_react_measurements(bindings):
    global measurements
    measurements += bindings
    return []


def start_storage_kb(kb_id, kb_name, kb_description, ke_endpoint):
    register_knowledge_base(kb_id, kb_name, kb_description, ke_endpoint)

    answer_measurements_ki = register_answer_knowledge_interaction(
        """
            ?sensor rdf:type saref:Sensor .
            ?measurement saref:measurementMadeBy ?sensor .
            ?measurement saref:isMeasuredIn saref:TemperatureUnit .
            ?measurement saref:hasValue ?temperature .
            ?measurement saref:hasTimestamp ?timestamp .
        """,
        "answer-measurements",
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

    start_handle_loop(
        {
            answer_measurements_ki: handle_answer_measurements,
            react_measurements_ki: handle_react_measurements,
        },
        kb_id,
        ke_endpoint,
    )


if __name__ == "__main__":
    add_sigterm_hook()

    start_storage_kb(
        "http://example.org/storage",
        "Storage",
        "Storage for measurement",
        "http://knowledge-engine:8280/rest/",
    )
