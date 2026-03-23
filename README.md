![Java](https://img.shields.io/badge/Java-24-000000.svg?style=flat&logo=openjdk&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-000000.svg?style=flat&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Compose_Desktop-000000.svg?style=flat&logo=jetpackcompose&logoColor=white)

<h1 align="center" title="A lightweight and standalone Haskell IDE powered by Compose Desktop">haskcore</h1>

<p align="center">A lightweight and standalone Haskell IDE powered by Compose Desktop</p>

<p align="center"><img src="media/preview.png" alt="Preview"></p>

## About

*My mission is to create the only Haskell IDE that is comfortable and contains all the necessary features to effectively
work with the language, whether you're a beginner or not.*

## Features

- A text editor built from scratch using a rope buffer and rendered with Skia

- Syntax highlighting with Tree-sitter

- Built-in Dracula and Alucard color schemes

- HLS (LSP) support

- GHC, Cabal, and Stack support

- Multi-window support

## Contribution

The project is being developed solo and requires no code contributions.

> [!TIP]
> You can support the development by leaving feedback or making a [donation](https://numq.github.io/support).

## Architecture

> [!NOTE]
> The application was designed using the [Reduce & Conquer](https://github.com/numq/reduce-and-conquer) architectural
> pattern

This project follows a highly modularized, layered architecture designed for strict isolation, testability, and
scalability.

```mermaid
graph TD
core[":core"]

feature_presentation[":feature:*:presentation"]
feature_core[":feature:*:core"]

platform[":platform:*"]

service[":service:*"]

service --> core

feature_core --> core
feature_core --> service

feature_presentation --> core
feature_presentation --> feature_core

platform --> core
platform --> service
platform --> feature_core
platform --> feature_presentation
```

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
For commercial licensing inquiries, please contact me directly via email.

___

<p align="center">
  <a href="https://numq.github.io/support">
    <img src="https://api.qrserver.com/v1/create-qr-code/?size=112x112&data=https://numq.github.io/support&bgcolor=1a1b26&color=7aa2f7" 
         width="112" 
         height="112" 
         style="border-radius: 4px;" 
         alt="QR code">
  </a>
  <br>
  <br>
  <a href="https://numq.github.io/support" style="text-decoration: none;">
    <code><font color="#bb9af7">numq.github.io/support</font></code>
  </a>
</p>