## Embedding service to showcase hexagonal architecture and domain-driven design in java

### Entities
Main aggregate root is note.java

Use H2 to store embeddings

Use spring boot data jdbc
Use Thymeleaf
Use Ollama local AI

Two use cases. 
1: Create embeddings from a text box in thymeleaf. Store in H2 table. Note.java is the main aggregate rich model
2: Ask AI for something. Use embeddings previously stored

Structure in packages: application, domain, infrastructure
