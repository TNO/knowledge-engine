import os
import logging
import json

log = logging.getLogger(__name__)
log.setLevel(logging.INFO)

from typing import Dict, List
from knowledge_mapper.utils import match_bindings
from knowledge_mapper.tke_client import TkeClient
from knowledge_mapper.knowledge_base import KnowledgeBaseRegistrationRequest
from knowledge_mapper.knowledge_interaction import (
    AnswerKnowledgeInteractionRegistrationRequest,
)

KE_URL = os.getenv("KE_URL")
KB_ID = os.getenv("KB_ID")
KB_NAME = KB_ID.split("/")[-1]
KB_DATA = json.loads(os.getenv("KB_DATA"))

log = logging.getLogger(KB_NAME)
log.setLevel(logging.INFO)


def answering_kb():
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
        log.info(f"ANSWER KI is handling a request...")
        return match_bindings(
            bindings,
            KB_DATA,
        )

    log.info(f"registering ANSWER KI...")
    kb.register_knowledge_interaction(
        AnswerKnowledgeInteractionRegistrationRequest(
            pattern="?a <http://example.org/relatedTo> ?b .", handler=handler
        )
    )
    log.info(f"ANSWER KI registered!")

    kb.start_handle_loop()

    log.info(f"unregistering...")
    kb.unregister()


if __name__ == "__main__":
    answering_kb()
