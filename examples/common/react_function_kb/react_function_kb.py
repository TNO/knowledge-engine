import os
import logging
import json

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
RESULT_GRAPH_PATTERN = os.getenv("RESULT_GRAPH_PATTERN")


def react(bindings):
    log.error("should be overridden by `exec`ed Python function definition!")


# This is a very hacky way to make the function configurable with an environment
# variable. Don't use stuff like this in production!
REACT_FUNCTION_DEF = os.getenv("REACT_FUNCTION_DEF")
exec(REACT_FUNCTION_DEF)

log = logging.getLogger(KB_NAME)
log.setLevel(logging.INFO)


def react_function_kb():
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

    kb.start_handle_loop()

    log.info(f"unregistering...")
    kb.unregister()


if __name__ == "__main__":
    react_function_kb()
