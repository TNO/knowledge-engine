import os
import logging
import json
import signal

log = logging.getLogger(__name__)
log.setLevel(logging.INFO)

from typing import Dict, List
from knowledge_mapper.tke_client import TkeClient
from knowledge_mapper.knowledge_base import KnowledgeBaseRegistrationRequest
from knowledge_mapper.knowledge_interaction import (
    ReactKnowledgeInteractionRegistrationRequest,
)

KE_URL = os.getenv("KE_URL")
KB_ID = os.getenv("KB_ID")
KB_NAME = KB_ID.split("/")[-1]
if "PREFIXES" in os.environ:
    PREFIXES = json.loads(os.getenv("PREFIXES"))
else:
    PREFIXES = None
ARGUMENT_GRAPH_PATTERN = os.getenv("ARGUMENT_GRAPH_PATTERN")
RESULT_GRAPH_PATTERN = None

def react(bindings):
    log.info(f"Reacting with empty bindingset to {bindings}...")
    result = []
    return result;


log = logging.getLogger(KB_NAME)
log.setLevel(logging.INFO)

...
"""
Make sure that this Python program handles SIGTERM by raising a
KeyboardInterrupt, to end the program.
"""
def handle_sigterm(*args):
    raise KeyboardInterrupt()

signal.signal(signal.SIGTERM, handle_sigterm)

def reacting_kb():
    client = TkeClient(KE_URL)
    client.connect()
    log.info(f"registering KB...")
    kb = client.register(
        KnowledgeBaseRegistrationRequest(
            id=KB_ID,
            name=KB_NAME,
            description=KB_ID.split("/")[-1],
        )
    )
    log.info(f"KB registered!")

    def handler(
        bindings: List[Dict[str, str]], requesting_kb_id: str
    ) -> List[Dict[str, str]]:
        log.info(f"REACT KI is handling a request...")
        return react(bindings)

    log.info(f"registering REACT KI...")
    kb.register_knowledge_interaction(
        ReactKnowledgeInteractionRegistrationRequest(
            argument_pattern=ARGUMENT_GRAPH_PATTERN,
            result_pattern=RESULT_GRAPH_PATTERN,
            prefixes=PREFIXES,
            handler=handler,
        )
    )
    log.info(f"REACT KI registered!")

    try:
        kb.start_handle_loop()
    except KeyboardInterrupt:
        log.info("Gracefull shutdown requested...")
    finally:
        kb.unregister()


if __name__ == "__main__":
    reacting_kb()
