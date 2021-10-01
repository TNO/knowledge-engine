FROM denoland/deno:1.14.2

WORKDIR /app

# Prefer not to run as root.
USER deno

COPY src .

# Compile the main app so that it doesn't need to be compiled each startup/entry.
RUN deno cache main.ts

CMD ["run", "--allow-net", "main.ts"]
