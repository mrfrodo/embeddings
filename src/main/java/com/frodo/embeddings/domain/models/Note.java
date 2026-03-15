package com.frodo.embeddings.domain.models;

/**
 * Main aggregate root. Rich model.
 */

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Note {

    // ── Identity ──────────────────────────────────────────────────────────────
    private final NoteId id;

    // ── Value objects ─────────────────────────────────────────────────────────
    private NoteContent content;
    private EmbeddingVector embeddingVector;   // null until embedded
    private ModelIdentifier modelIdentifier;   // which Ollama model was used
    private EmbeddingStatus status;

    // ── Audit ─────────────────────────────────────────────────────────────────
    private final Instant createdAt;
    private Instant embeddedAt;

    // ── Domain events (cleared after publishing) ──────────────────────────────
    private final List<Object> domainEvents = new ArrayList<>();

    // ── Factory method ────────────────────────────────────────────────────────
    public static Note create(NoteId id, NoteContent content) {
        Note note = new Note(id, content);
        note.domainEvents.add(new NoteEmbeddingRequested(id, content));
        return note;
    }

    private Note(NoteId id, NoteContent content) {
        this.id              = NoteId.requireNonNull(id);
        this.content         = NoteContent.requireNonNull(content);
        this.status          = EmbeddingStatus.PENDING;
        this.createdAt       = Instant.now();
    }

    // ── Core behaviour ────────────────────────────────────────────────────────

    /**
     * Attach the embedding vector returned by Ollama.
     * Enforces: can only be called once; vector dimensions must be positive.
     */
    public void embedNote(EmbeddingVector vector, ModelIdentifier model) {
        guardNotAlreadyEmbedded();
        this.embeddingVector  = EmbeddingVector.requireNonNull(vector);
        this.modelIdentifier  = ModelIdentifier.requireNonNull(model);
        this.status           = EmbeddingStatus.EMBEDDED;
        this.embeddedAt       = Instant.now();
        domainEvents.add(new NoteEmbedded(id, vector, model, embeddedAt));
    }

    /**
     * Mark the note as failed if Ollama is unreachable or returns an error.
     */
    public void markFailed(String reason) {
        guardNotAlreadyEmbedded();
        this.status = EmbeddingStatus.FAILED;
        domainEvents.add(new NoteEmbeddingFailed(id, reason, Instant.now()));
    }

    /**
     * Cosine similarity against another vector.
     * Invariant: note must be embedded before similarity can be computed.
     */
    public double cosineSimilarityTo(EmbeddingVector other) {
        guardEmbedded();
        return this.embeddingVector.cosineSimilarity(other);
    }

    public boolean isEmbedded() {
        return status == EmbeddingStatus.EMBEDDED;
    }

    // ── Invariant guards ──────────────────────────────────────────────────────

    private void guardEmbedded() {
        if (!isEmbedded()) {
            throw new NoteNotEmbeddedException(id,
                    "Note must be embedded before similarity can be computed.");
        }
    }

    private void guardNotAlreadyEmbedded() {
        if (isEmbedded()) {
            throw new NoteAlreadyEmbeddedException(id,
                    "Note has already been embedded with model: " + modelIdentifier);
        }
    }

    // ── Domain event support ─────────────────────────────────────────────────

    public List<Object> pullDomainEvents() {
        List<Object> events = Collections.unmodifiableList(new ArrayList<>(domainEvents));
        domainEvents.clear();
        return events;
    }

    // ── Getters (no setters — state changes go through behaviour methods) ─────

    public NoteId getId()                       { return id; }
    public NoteContent getContent()             { return content; }
    public EmbeddingVector getEmbeddingVector() { return embeddingVector; }
    public ModelIdentifier getModelIdentifier() { return modelIdentifier; }
    public EmbeddingStatus getStatus()          { return status; }
    public Instant getCreatedAt()               { return createdAt; }
    public Instant getEmbeddedAt()              { return embeddedAt; }
}
