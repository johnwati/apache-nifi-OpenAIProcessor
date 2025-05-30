# 🚀 Apache NiFi Custom OpenAI Processor

This project provides a custom Apache NiFi processor that integrates with OpenAI's API. It accepts flow file content as input, sends it to OpenAI (e.g., GPT-4), and returns the response as flow file content for downstream processing.

---

## 📦 Project Structure

```
nifi-openai-processor/
├── openai-processors/            # Custom processor Java module
├── openai-nar/                   # Bundles processor into a NAR file
├── target/                       # Output NAR file after build
nifi-docker/
├── docker-compose.yml           # Docker Compose setup
├── nar/
│   └── nifi-openai-nar-1.0.0.nar # Copy of built NAR file
```

---

## ✅ Prerequisites

- Java 8 or 11
- Apache Maven
- Docker and Docker Compose
- OpenAI API Key

---

## 🛠️ Step 1: Generate Processor Project

Run the following Maven archetype command to scaffold your NiFi processor project:

```bash
mvn archetype:generate \
  -DarchetypeGroupId=org.apache.nifi \
  -DarchetypeArtifactId=nifi-processor-bundle-archetype \
  -DarchetypeVersion=1.23.0 \
  -DgroupId=org.example.nifi \
  -DartifactId=nifi-openai-processor \
  -Dversion=1.0.0 \
  -DartifactBaseName=openai \
  -DnifiVersion=1.23.0 \
  -DinteractiveMode=false
```

This will create a new directory called `nifi-openai-processor` with the correct module structure.

---

## 🛠️ Step 2: Implement the Processor

Inside the generated module:

```bash
cd nifi-openai-processor/openai-processors
```

Modify the `OpenAIProcessor.java` file under `src/main/java/org/example/nifi/processors/` to implement:

- Reading flow file content
- Sending a request to OpenAI’s API
- Writing the response to the flow file

---

## 🛠️ Step 3: Build the Processor

In the root project directory (`nifi-openai-processor`):

```bash
mvn clean install
```

This will generate a `.nar` file at:

```bash
openai-nar/target/nifi-openai-nar-1.0.0.nar
```

Copy it to your Docker setup:

```bash
mkdir -p ../nifi-docker/nar
cp openai-nar/target/nifi-openai-nar-1.0.0.nar ../nifi-docker/nar/
```

---

## 🐳 Step 4: Docker Compose Setup

### 📄 docker-compose.yml

```yaml
version: '3.8'

services:
  nifi:
    image: apache/nifi:1.23.0
    container_name: nifi-openai
    ports:
      - "8080:8080"
    environment:
      - NIFI_WEB_HTTP_PORT=8080
    volumes:
      - ./nar:/opt/nifi/nifi-current/lib
    restart: unless-stopped
```

### Run the container:

```bash
cd ../nifi-docker
docker-compose up -d
```

Access the NiFi UI at:

```
http://localhost:8080/nifi
```

---

## ⚙️ Step 5: Use the Processor in NiFi

1. In the NiFi UI, click **Add Processor** (`+`)
2. Search for `OpenAIProcessor`
3. Drag it onto the canvas
4. Configure it with your OpenAI API key and desired settings

---

## 🔄 Update and Reload

When making changes:

1. Rebuild with `mvn clean install`
2. Replace the NAR file
3. Restart Docker:

```bash
docker-compose restart
```

---

## 🔐 Security Tips

- Use `Controller Services` or encrypted properties for API keys
- Use HTTPS in production

---

## 📚 Future Ideas

- Add support for conversation history
- Parameterize prompts
- Handle OpenAI rate limits and errors

---

## 📝 License

MIT License
