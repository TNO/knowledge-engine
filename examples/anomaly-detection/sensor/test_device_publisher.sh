#!/bin/bash

# Script to test the basic functionaly of the knowledge engine adaptor

register_kb() {
    OUT=$(curl -s $KE_URL/sc \
         -X POST \
         -H "Content-Type: application/json" \
         -d @- << EOF
{
    "knowledgeBaseId": "http://example.org/$SENSOR_ID-kb",
    "knowledgeBaseName": "Test Temperature Sensor",
    "knowledgeBaseDescription": "This is a temperature sensor simulator for testing purposes."
}
EOF
) 2>/dev/null

    echo $?
}

register_ki() {
    OUT=$(curl -s $KE_URL/sc/ki \
         -X POST \
         -H "Content-Type: application/json" \
         -H "Knowledge-Base-Id: http://example.org/$SENSOR_ID-kb" \
         -d @- << EOF
{
    "knowledgeInteractionType": "PostKnowledgeInteraction",
    "knowledgeInteractionName": "TemperatureObservations",
    "argumentGraphPattern": "?m rdf:type ex:Measurement .
                             ?m ex:hasValueInCelsius ?celsius .
                             ?m ex:madeBy ex:$SENSOR_ID .
                             ex:$SENSOR_ID rdf:type ex:$SENSOR_TYPE .",
    "prefixes":
    {
      "ex": "http://example.org/",
      "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    }
}
EOF
) 2>/dev/null

    echo $?
}

post_data() {
    VALUE="$1"
    TIMESTAMP=$(date +"%Y-%m-%dT%T")

    OUT=$(curl -s $KE_URL/sc/post \
         -X POST \
         -H "Content-Type: application/json" \
         -H "Knowledge-Base-Id: http://example.org/$SENSOR_ID-kb" \
         -H "Knowledge-Interaction-Id: http://example.org/$SENSOR_ID-kb/interaction/TemperatureObservations"\
         -d @- << EOF
[
    {
        "m": "<http://example.org/$SENSOR_ID-kb/measurement>",
        "celsius": "\"$VALUE\"^^<http://www.w3.org/2001/XMLSchema#float>"
    }
]
EOF
) 2>/dev/null
    echo $?
}

echo "Attempting to register knowledge base at $KE_URL"
RET=$(register_kb)
if [ $RET -gt 0 ]
then
    echo "Unable to register knowledge base: $RET"

    exit 3
fi

sleep 1
echo "Attempting to register knowledge interaction at $KE_URL"
RET=$(register_ki)
if [ $RET -gt 0 ]
then
    echo "Unable to register knowledge interaction: $RET"

    exit 4
fi

sleep 1
echo "Attempting to post sensor data"

i=1
while true
do
    if [ $(($i % $ANOMALY_FREQUENCY)) -eq 0 ]
    then
        VALUE=$ANOMALOUS_VALUE
        echo " $i - Publishing temperature: $VALUE % (simulating faulty sensor)"
    else
        VALUE=$(($MIN_VALUE + $RANDOM % $DELTA_VALUE)).$(($RANDOM % 10))
        echo " $i - Publishing temperature: $VALUE $UNIT"
    fi

    RET=$(post_data "$VALUE")
    if [ $RET -gt 0 ]
    then
        echo "\nUnable to post temperature"

        exit 5
    fi

    ((i=i+1))
    sleep $SLEEPTIME
done

echo 0
