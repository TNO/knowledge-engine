FROM python:3.10.6-alpine

# Create and enable venv
RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

RUN pip install --upgrade pip

WORKDIR /app/

COPY requirements.txt .
RUN pip install -r requirements.txt

COPY ./kb2.py .

ENTRYPOINT [ "python", "kb2.py" ]
