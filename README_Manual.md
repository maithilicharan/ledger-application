# User Manual for Ledger Application REST API

## 1. Introduction

This manual provides instructions on how to use the Ledger Application's REST API. The API allows you to perform various
operations such as transferring funds, changing account status, modifying posting status, and viewing historical
balance.

## 2. Transfer Funds

To transfer funds between accounts, send a `POST` request to the `/transfer` endpoint.

**Request:**

```http
POST /transfer
Content-Type: application/json

{
  "entityId": "entity-id",
  "sourceId": "source-id",
  "destinationId": "destination-id",
  "amount": amount-to-transfer
}
```

## 3. Change Account Status

To change the status of an account, send a `PUT` request to
the `/change-account-status/entity/{entity-id}/new-account-status/{new-account-status}` endpoint.

**Request:**

```http
PUT /change-account-status/entity/{entity-id}/new-account-status/{new-account-status}
```

## 4. Modify Posting Status

To modify the status of a posting, send a `PUT` request to
the `/change-posting-status/posting/{posting-id}/new-posting-status/{new-posting-status}` endpoint.

**Request:**

```http
PUT /change-posting-status/posting/{posting-id}/new-posting-status/{new-posting-status}
```

## 5. View Historical Balance

To view the historical balance of an account, send a `GET` request to the `/account/{account-id}/historical-balance`
endpoint.

**Request:**

```http
GET /account/{account-id}/historical-balance
```
