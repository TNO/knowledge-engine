import datetime
import time
import random
import logging

from utils import *

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def generate_random_temperature(start=15.0, end=20.0):
    assert end > start
    return start + (end - start) * random.random()


def start_sensor_kb(kb_id, kb_name, kb_description, ke_endpoint):
    register_knowledge_base(kb_id, kb_name, kb_description, ke_endpoint)
    ki_id = register_post_knowledge_interaction(
        """
            ?sensor rdf:type saref:Sensor .
            ?measurement saref:measurementMadeBy ?sensor .
            ?measurement saref:isMeasuredIn saref:TemperatureUnit .
            ?measurement saref:hasValue ?temperature .
            ?measurement saref:hasTimestamp ?timestamp .
        """,
        None,
        "post-measurements",
        kb_id,
        ke_endpoint,
        {
            "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
            "saref": "https://saref.etsi.org/core/",
        },
    )

    measurement_counter = 0
    while True:
        now = datetime.datetime.now()
        measurement_counter += 1
        value = generate_random_temperature(12, 26)

        post(
            [
                {
                    "sensor": "<https://example.org/sensor/1>",
                    "measurement": f"<https://example.org/sensor/1/measurement/{measurement_counter}>",
                    "temperature": f"{value}",
                    "timestamp": f'"{now.isoformat()}"',
                }
            ],
            ki_id,
            kb_id,
            ke_endpoint,
        )
        logger.info(f"published measurement of {value} units at {now.isoformat()}")

        time.sleep(2)


if __name__ == "__main__":
    add_sigterm_hook()

    start_sensor_kb(
        "http://example.org/sensor",
        "Sensor",
        "A temperature sensor",
        "http://knowledge-engine:8280/rest/",
    )
