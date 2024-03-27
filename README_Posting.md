# Design Document for Client Modifying Posting State

## 1. Introduction

This document outlines the design for the feature that allows clients to modify the `PostingState`.
The `PostingState` can be  `PENDING`, `FAILED`, or `CLEARED`.

## 2. Requirements

- The system should allow clients to modify the state of their own posting.
- The system should validate the new state before updating.
- The system should log all state modifications for auditing purposes.

## 3. Design Details

### 3.1 API Endpoint

We will expose a REST API
endpoint `PUT /change-posting-status/posting/{posting-id}/new-posting-status/{new-posting-status}` to update the state
of a client's posting.

### 3.2 Input

The API will accept a path variable with the new Posting Status.

### 3.3 Processing

- The system will validate the new state.
- The system will check if the client is authorized to modify their posting state.
- If validation passes, the system will update the posting state.
