import os
import logging
import time

from knowledge_mapper.tke_client import TkeClient
from knowledge_mapper.knowledge_base import KnowledgeBaseRegistrationRequest
from knowledge_mapper.knowledge_interaction import (
    AskKnowledgeInteraction,
    AskKnowledgeInteractionRegistrationRequest,
)

KE_URL = os.getenv("KE_URL")
KB_ID = os.getenv("KB_ID")
KB_NAME = KB_ID.split("/")[-1]

log = logging.getLogger(KB_NAME)
log.setLevel(logging.INFO)


def kb_1():
    client = TkeClient(KE_URL)
    client.connect()
    log.info(f"registering KB...")
    kb = client.register(
        KnowledgeBaseRegistrationRequest(
            id=f"{KB_ID}",
            name=f"{KB_NAME}",
            description=f"{KB_NAME}",
        )
    )
    log.info(f"KB registered!")
    log.info(f"registering ASK KI...")
    ask: AskKnowledgeInteraction = kb.register_knowledge_interaction(
        AskKnowledgeInteractionRegistrationRequest(
            pattern="?a <http://example.org/relatedTo> ?b ."
        )
    )
    log.info(f"ASK KI registered!")
    result = []
    while True:
        log.info(f"asking...")
        result = ask.ask([{}])["bindingSet"]
        if len(result) == 0:
            log.info(f"asking gave no results; will sleep for 2s...")
        else:
            log.info(f"got answer: {result}")
        time.sleep(2)

    log.info(f"unregistering...")
    kb.unregister()


if __name__ == "__main__":
    kb_1()
