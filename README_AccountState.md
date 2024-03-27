# Design Document for Client Modifying Account State

## 1. Introduction

This document outlines the design for the feature that allows clients to modify the `AccountState`.
The `AccountState` can be `OPEN`, `CLOSED`, `SUSPENDED`, or `FROZEN`.

## 2. Requirements

- The system should allow clients to modify the state of their own account.
- The system should validate the new state before updating.
- The system should log all state modifications for auditing purposes.

## 3. Design Details

### 3.1 API Endpoint

We will expose a REST API
endpoint `PUT /change-account-status/entity/{entity-id}/new-account-status/{new-account-status}`
to update the state of a client's account.

### 3.2 Input

The API will accept a path variable with the new Account Status.

### 3.3 Processing

- The system will validate the new state.
- The system will check if the client is authorized to modify their account state.
- If validation passes, the system will update the account state.

