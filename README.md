# FinancialApp

A personal-finance tracker for Android that treats spending control as a game: you log
expenses, complete quests, earn achievements, and watch reports instead of filling in a
spreadsheet.

## What it does

- **Expenses** — add, categorise and browse spending, with reports over time
- **Receipt scanning** — photograph a receipt and let OCR fill the entry in, through a
  hosted OCR service ([receipt-ocr](https://huggingface.co/spaces/Zonda001/receipt-ocr),
  also mine)
- **Quests and achievements** — goals with progress and badges, so the habit has a loop
- **Trading positions** — a simple portfolio view backed by a live price API
- **Accounts** — registration, login, and biometric unlock
- **Settings** — persisted with DataStore

## Stack

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Room · Coroutines ·
DataStore · AndroidX Biometric · MVVM

`minSdk 24`, `targetSdk 36`.

## Architecture

Three layers, no framework magic:

```
data/
  api/         HuggingFace OCR service, price API
  local/       Room database — DAOs and entities
  repository/  the only thing the UI layer talks to
  settings/    DataStore preferences
ui/
  screens/     one package per screen, each with its own ViewModel
  components/  reusable Compose pieces
  navigation/  NavGraph
```

Screens never touch a DAO or an HTTP call directly — they hold a ViewModel, the ViewModel
holds a repository, and the repository decides whether an answer comes from Room or from
the network.

## Build

```bash
git clone https://github.com/Zonda001/FinancialApp.git
```

Open in Android Studio and run — Gradle wrapper included, no extra setup.
