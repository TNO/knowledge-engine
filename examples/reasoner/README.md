# Reasoning to orchestrate data exchange: converting units

This is an example of reasoning to orchestrate data exchange.

The example consists of three knowledge bases:

- `kb1`: A knowledge base interested in temperature measurements in celsius
  - It simply asks for all celsius measurements every few seconds, and prints the results
- `kb2`: A knowledge base that has temperature measurements in fahrenheit
  - It makes available the data in `KB_DATA`, using the pattern in `GRAPH_PATTERN`
- `kb3`: A knowledge base that can convert fahrenheit measurements into celsius measurements
  - It converts bindings in the `ARGUMENT_PATTERN` form into bindings in the `RESULT_PATTERN` form, using the Python function `react` defined in `REACT_FUNCTION_DEF`.

When running the project, and showing the logs of the `kb1` service:

```
docker compose up -d
docker compose logs -f kb1
```

You see that it receives the measurements in celsius:

```
INFO:kb1:registering KB...
INFO:kb1:KB registered!
INFO:kb1:registering ASK KI...
INFO:kb1:ASK KI registered!
INFO:kb1:asking...
INFO:kb1:got answer: [{'celsius': '20.11111111111111', 'm': '<http://example.org/data/measurement>'}]
```
