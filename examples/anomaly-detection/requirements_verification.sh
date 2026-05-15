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

NCOLS=$(($(tput cols)-5))
RED=$(tput setaf 1)
GREEN=$(tput setaf 2)
NORMAL=$(tput sgr0)

GRACE_PERIOD=130

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
        # all knowledge nodes registered their publications and subscriptions
	    return 0
    fi

    return 1
}

test_RQ.KE-3() {
    NUM_EXPECTED=5

    num_matches=$(docker compose logs --tail=250 | grep -o "Planning post for KI <.*>" | sort | uniq | wc -l)
    if [[ "$num_matches" -eq $NUM_EXPECTED ]]
    then
        # received data from all registered publications
	    return 0
    fi

    return 1
}

test_RQ.KE-4() {
    NUM_EXPECTED=2

    num_matches=$(docker compose logs --tail=250 | grep -o "Contacting my KB to react to KI <.*>"| sort | uniq | wc -l)
    if [[ "$num_matches" -eq $NUM_EXPECTED ]]
    then
        # send data to all registered subscribers
    	return 0
    fi

    return 1
}

test_RQ.KE-5() {
    NUM_EXPECTED=3

    num_matches=$(docker compose logs --tail=250 | grep -o "Finished post for KI <.*> with .* result bindings involving 1 KI(s) (of which .* failed: .*"| sort | uniq | wc -l)
    if [[ "$num_matches" -eq $NUM_EXPECTED ]]
    then
        # forward data from registered publishers to registered subscribers
    	return 0
    fi

    return 1
}

test_RQ.KV-1() {
    num_matches=$(docker compose logs knowledge-validator-kb | grep -cF "Received new graph message")
    if [[ "$num_matches" -gt 0 ]]
    then
        # received at least one message
        return 0
    fi

    return 1
}

test_RQ.KV-2() {
    num_matches=$(docker compose logs knowledge-validator-kb | grep -cF "Creating validation report")
    if [[ "$num_matches" -gt 0 ]]
    then
        # learned at least one pattern of nominal behaviour
        return 0
    fi

    return 1
}

test_RQ.KV-3() {
    num_matches=$(docker compose logs knowledge-validator-kb | grep -cF "Updating graph data")
    if [[ "$num_matches" -gt 0 ]]
    then
        # updated at least one pattern
        return 0
    fi

    return 1
}

test_RQ.KV-4() {
    num_matches=$(docker compose logs knowledge-validator-kb | grep -cF "Graph failed validation")
    if [[ "$num_matches" -gt 0 ]]
    then
        # detected at least one anomaly
        return 0
    fi

    return 1
}

test_RQ.KV-5() {
    num_matches=$(docker compose logs knowledge-validator-kb | grep -cF "Publishing validation report")
    if [[ "$num_matches" -gt 0 ]]
    then
        # sent at least one validation report
        return 0
    fi

    return 1
}

test_RQ.KV-6() {
    num_matches=$(docker compose logs knowledge-validator-kb | grep -c "http://www.w3.org/ns/shacl#resultMessage, .*$")
    if [[ "$num_matches" -gt 0 ]]
    then
        # received at least one message
        return 0
    fi

    return 1
}


run_tests() {
    # Run unit tests.
    # 0 = success
    # > 0 = failure
    
    num_failed=0
   
    echo "Verifying task requirements"
    if [[ "$1" != "-now" ]]
    then
        echo " Waiting $GRACE_PERIOD seconds to allow pattern learning (use '-now' to skip)"
        sleep $GRACE_PERIOD
    fi
   
    message=" RQ.KE-1 - Process node registration requests "
    length=${#message}
    echo -n "$message"
    padding=$(($NCOLS-$length))

    test_RQ.KE-1
    if [[ $? -eq 0 ]]
    then
        printf '%s%*s%s\n' "$GREEN" $padding "[PASSED]" "$NORMAL"
    else
        printf '%s%*s%s\n' "$RED" $padding "[FAILED]" "$NORMAL"
        ((num_failed=num_failed+1))
    fi

    sleep 1
    message=" RQ.KE-2 - Process publication and subscription requests "
    length=${#message}
    echo -n "$message"
    padding=$(($NCOLS-$length))

    test_RQ.KE-2
    if [[ $? -eq 0 ]]
    then
        printf '%s%*s%s\n' "$GREEN" $padding "[PASSED]" "$NORMAL"
    else
        printf '%s%*s%s\n' "$RED" $padding "[FAILED]" "$NORMAL"
        ((num_failed=num_failed+1))
    fi

    sleep 1
    message=" RQ.KE-3 - Accept data from registered publishers "
    length=${#message}
    echo -n "$message"
    padding=$(($NCOLS-$length))

    test_RQ.KE-3
    if [[ $? -eq 0 ]]
    then
        printf '%s%*s%s\n' "$GREEN" $padding "[PASSED]" "$NORMAL"
    else
        printf '%s%*s%s\n' "$RED" $padding "[FAILED]" "$NORMAL"
        ((num_failed=num_failed+1))
    fi

    sleep 1
    message=" RQ.KE-4 - Send data to registered subscribers "
    length=${#message}
    echo -n "$message"
    padding=$(($NCOLS-$length))

    test_RQ.KE-4
    if [[ $? -eq 0 ]]
    then
        printf '%s%*s%s\n' "$GREEN" $padding "[PASSED]" "$NORMAL"
    else
        printf '%s%*s%s\n' "$RED" $padding "[FAILED]" "$NORMAL"
        ((num_failed=num_failed+1))
    fi

    sleep 1
    message=" RQ.KE-5 - Forward data from registered publishers to registered subscribers"
    length=${#message}
    echo -n "$message"
    padding=$(($NCOLS-$length))

    test_RQ.KE-5
    if [[ $? -eq 0 ]]
    then
        printf '%s%*s%s\n' "$GREEN" $padding "[PASSED]" "$NORMAL"
    else
        printf '%s%*s%s\n' "$RED" $padding "[FAILED]" "$NORMAL"
        ((num_failed=num_failed+1))
    fi

    sleep 1
    message=" RQ.KV-1 - Real-time monitoring of data streams "
    length=${#message}
    echo -n "$message"
    padding=$(($NCOLS-$length))

    test_RQ.KV-1
    if [[ $? -eq 0 ]]
    then
        printf '%s%*s%s\n' "$GREEN" $padding "[PASSED]" "$NORMAL"
    else
        printf '%s%*s%s\n' "$RED" $padding "[FAILED]" "$NORMAL"
        ((num_failed=num_failed+1))
    fi

    sleep 1
    message=" RQ.KV-2 - Learn patterns of nominal behaviour "
    length=${#message}
    echo -n "$message"
    padding=$(($NCOLS-$length))

    test_RQ.KV-2
    if [[ $? -eq 0 ]]
    then
        printf '%s%*s%s\n' "$GREEN" $padding "[PASSED]" "$NORMAL"
    else
        printf '%s%*s%s\n' "$RED" $padding "[FAILED]" "$NORMAL"
        ((num_failed=num_failed+1))
    fi

    sleep 1
    message=" RQ.KV-3 - Update patterns of nominal behaviour "
    length=${#message}
    echo -n "$message"
    padding=$(($NCOLS-$length))

    test_RQ.KV-3
    if [[ $? -eq 0 ]]
    then
        printf '%s%*s%s\n' "$GREEN" $padding "[PASSED]" "$NORMAL"
    else
        printf '%s%*s%s\n' "$RED" $padding "[FAILED]" "$NORMAL"
        ((num_failed=num_failed+1))
    fi

    sleep 1
    message=" RQ.KV-4 - Detect deviations from nominal behaviour "
    length=${#message}
    echo -n "$message"
    padding=$(($NCOLS-$length))

    test_RQ.KV-4
    if [[ $? -eq 0 ]]
    then
        printf '%s%*s%s\n' "$GREEN" $padding "[PASSED]" "$NORMAL"
    else
        printf '%s%*s%s\n' "$RED" $padding "[FAILED]" "$NORMAL"
        ((num_failed=num_failed+1))
    fi

    sleep 1
    message=" RQ.KV-5 - Report on detected deviations "
    length=${#message}
    echo -n "$message"
    padding=$(($NCOLS-$length))

    test_RQ.KV-5
    if [[ $? -eq 0 ]]
    then
        printf '%s%*s%s\n' "$GREEN" $padding "[PASSED]" "$NORMAL"
    else
        printf '%s%*s%s\n' "$RED" $padding "[FAILED]" "$NORMAL"
        ((num_failed=num_failed+1))
    fi

    sleep 1
    message=" RQ.KV-6 - Provide explanations for detected deviations "
    length=${#message}
    echo -n "$message"
    padding=$(($NCOLS-$length))

    test_RQ.KV-6
    if [[ $? -eq 0 ]]
    then
        printf '%s%*s%s\n' "$GREEN" $padding "[PASSED]" "$NORMAL"
    else
        printf '%s%*s%s\n' "$RED" $padding "[FAILED]" "$NORMAL"
        ((num_failed=num_failed+1))
    fi

    echo "Number of tests failed: $num_failed"
}

run_tests "$1"

