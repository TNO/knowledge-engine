# REST API example

This Docker Compose project contains three knowledge bases implemented in Python that connect to the Knowledge Engine through the REST API.

The setup of the knowledge bases is illustrated as follows:

![](./img/kbs.svg)

*Arrows that point up are proactive interactions whereas arrows that point down are reactive interactions.*

All knowledge interactions in this setup use the same graph pattern (so the ASK/ANSWER and POST/REACT pairs all match):

```
?sensor rdf:type saref:Sensor .
?measurement saref:measurementMadeBy ?sensor .
?measurement saref:isMeasuredIn saref:TemperatureUnit .
?measurement saref:hasValue ?temperature .
?measurement saref:hasTimestamp ?timestamp .
```

## Temperature sensor
The temperature sensor knowledge base (called `sensor` in the example) regularly publishes temperature measurements through a POST knowledge interaction.

## Database
The database (called `storage` in the example) listens for new measurements, and can be queried for historical measurements.

## GUI
The GUI (called `ui` in the example) commences by retrieving all historical measurements and displaying them. Then it starts listening for live updates that it also displays.

# Running it

To run the example, and only show the UI results, do the following:

```
docker-compose up --build --force-recreate -d && docker-compose logs -f ui
```

When you're done, terminate it with:

```
docker-compose down
```
