#!/bin/bash

# This script contains unit tests to verify the task requirements
# pertaining to the Knowledge Engine and Knowledge Validator.
#
# REQUIREMENTS
#
# RQ.KE-1 - Process node registration requests
# RQ.KE-2 - Process publication and subscription requests
# RQ.KE-3 - Accept data from registered publishers
# RQ.KE-4 - Send data to registered subscribers
# RQ.KE-5 - Forward data from registered publishers to registered subscribers 
# 
# RQ.KV-1 - Real-time monitoring of data streams
# RQ.KV-2 - Learn patterns of nominal behaviour
# RQ.KV-3 - Update patterns of nominal behaviour
# RQ.KV-4 - Detect deviations from nominal behaviour
# RQ.KV-5 - Report on detected deviations
# RQ.KV-6 - Provide explanations for detected deviations

KV_NAME="anomaly-detection-kb"

test_RQ.KE-1() {
    NUM_EXPECTED=8

    num_matches=$(docker compose logs | grep -cF "SC communication ready took")
    if [[ "$num_matches" -eq $NUM_EXPECTED ]]
    then
        # all knowledge nodes were registered successfully
        return 0
    fi

    return 1
}

test_RQ.KE-2() {
    NUM_EXPECTED=9 
    num_matches=$(docker compose logs | grep -cF "Knowledge interaction created in KB")
    if [[ "$num_matches" -eq $NUM_EXPECTED ]]
    then
        #all knowledge nodes registered their publications and subscriptions
	return 0
    fi

    return 1
}

test_RQ.KE-3() {
    NUM_EXPECTED=5 
    num_matches=$(docker compose logs | grep -o "Planning post for KI <.*>" | sort | uniq | wc -l)
    if [[ "$num_matches" -eq $NUM_EXPECTED ]]
    then
        #received data from all registered publications
	return 0
    fi

    return 1
}
test_RQ.KE-4() {
    NUM_EXPECTED=2
    num_matches=$(docker compose logs|grep -o "Contacting my KB to react to KI <.*>"| sort | uniq | wc -l)
    if [[ "$num_matches" -eq $NUM_EXPECTED ]]
    then
        #send data to all registered subscribers
	return 0
    fi

    return 1
}
test_RQ.KE-5() {
    NUM_EXPECTED=3
    num_matches=$(docker compose logs|grep -o "Finished post for KI <.*> with .* result bindings involving 1 KI(s) (of which .* failed: .*"| sort | uniq | wc -l)
    if [[ "$num_matches" -eq $NUM_EXPECTED ]]
    then
        #forward data from registered publishers to registered subscribers
	return 0
    fi

    return 1
}

test_RQ.KV-1() {
    num_matches=$(docker compose logs "$KV_NAME" | grep -cF "")
    if [[ "$num_matches" -gt 0 ]]
    then
        return 0
    fi

    return 1
}

run_tests() {
    # Run unit tests.
    # 0 = success
    # > 0 = failure
    
    test_RQ.KE-1
    echo "test_RQ.KE-1: $?"

    test_RQ.KE-2
    echo "test_RQ.KE-2: $?"

    test_RQ.KE-3
    echo "test_RQ.KE-3: $?"

    test_RQ.KE-4
    echo "test_RQ.KE-4: $?"

    test_RQ.KE-5
    echo "test_RQ.KE-5: $?"

    test_RQ.KV-1
    echo "test_RQ.KV-1: $?"
}

run_tests

