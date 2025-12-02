import os
import logging
import time
import json
import signal

from knowledge_mapper.utils import match_bindings
from knowledge_mapper.tke_client import TkeClient
from knowledge_mapper.knowledge_base import KnowledgeBaseRegistrationRequest
from knowledge_mapper.knowledge_interaction import (
    PostKnowledgeInteraction,
    PostKnowledgeInteractionRegistrationRequest,
)

KE_URL = os.getenv("KE_URL")
KB_ID = os.getenv("KB_ID")
KB_NAME = KB_ID.split("/")[-1]
KB_DATA = json.loads(os.getenv("KB_DATA"))
if "PREFIXES" in os.environ:
    PREFIXES = json.loads(os.getenv("PREFIXES"))
else:
    PREFIXES = None
ARGUMENT_GRAPH_PATTERN = os.getenv("ARGUMENT_GRAPH_PATTERN")
RESULT_GRAPH_PATTERN = os.getenv("RESULT_GRAPH_PATTERN")

log = logging.getLogger(KB_NAME)
log.setLevel(logging.INFO)

"""
Make sure that this Python program handles SIGTERM by raising a
KeyboardInterrupt, to end the program.
"""
def handle_sigterm(*args):
    raise KeyboardInterrupt()

signal.signal(signal.SIGTERM, handle_sigterm)

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
    log.info(f"registering POST KI...")
    post: PostKnowledgeInteraction = kb.register_knowledge_interaction(
        PostKnowledgeInteractionRegistrationRequest(
            argument_pattern=ARGUMENT_GRAPH_PATTERN, result_pattern=RESULT_GRAPH_PATTERN, prefixes=PREFIXES
        )
    )
    log.info(f"POST KI registered!")
    result = []
    try:
	    while True:
	        log.info(f"posting...")
	        result = post.post(KB_DATA)
	        resultBindingSet = result["resultBindingSet"]
	        exchangeInfo = result["exchangeInfo"]
	        kbs = [ exchange['knowledgeBaseId'] for exchange in exchangeInfo]
	
	        if len(result) == 0:
	            log.debug(f"posting gave no results; will sleep for 2s...")
	            message = f"empty bindingset"
	        else:
	            message = f"{resultBindingSet}"
	            log.debug(f"got reaction: {resultBindingSet}")
	
	        log.info(f"Received {message} from following KBs: {kbs}")
	        time.sleep(2)
    finally:
        log.info(f"unregistering...")
        kb.unregister()


if __name__ == "__main__":
    kb_1()
