/**
 * Shared API contract between backend-local (one instance per country) and
 * backend-central (headquarters aggregator).
 *
 * <p>Scope rule: a type belongs here only if <b>both</b> Java services use it.
 * The consolidated envelopes that only backend-central produces
 * ({@code CountryGroup}, {@code CountryPageGroup}, {@code PageDto}) therefore stay
 * in backend-central, and persistence entities stay in backend-local.
 *
 * <p>Field names and enum constants are the wire format. They are mirrored in
 * {@code frontend-web/src/types/api.ts} and documented in
 * {@code backend-central/api-contract.md} — changing one means changing all three.
 * Domain vocabulary stays French per {@code docs/GLOSSAIRE.md}; everything else
 * (comments, javadoc, technical identifiers) is English.
 */
package com.futurekawa.lib;
