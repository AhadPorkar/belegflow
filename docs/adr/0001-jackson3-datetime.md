# ADR 0001 - Jackson 3 date configuration

Status: implemented in 0.2.0. Context: the supplied CI failed to bind a Jackson 2 SerializationFeature property during application startup. Decision: configure the Jackson 3 datetime datatype feature, keep ISO date/instant JSON, and load real application YAML in database-free regression tests. Reject downgrading libraries, suppressing startup errors or skipping database tests. No database migration is needed. Consequence: intentional JSON date changes require explicit API and client tests.
