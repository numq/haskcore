<h1 align="center" title="Haskell IDE - Modern Desktop Development Environment">haskcore</h1>

![Java](https://img.shields.io/badge/Java-24-000000.svg?style=flat&logo=openjdk&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-000000.svg?style=flat&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Compose_Desktop-000000.svg?style=flat&logo=jetpackcompose&logoColor=white)
![Haskell](https://img.shields.io/badge/Haskell-LSP%20Support-black)
![Status](https://img.shields.io/badge/Status-WIP-000000.svg?style=flat)

|                                                                  🖤                                                                   |                  Support this project                   |               
|:-------------------------------------------------------------------------------------------------------------------------------------:|:-------------------------------------------------------:|
|  <img src="https://raw.githubusercontent.com/ErikThiart/cryptocurrency-icons/master/32/bitcoin.png" alt="Bitcoin (BTC)" width="32"/>  | <code>bc1qs6qq0fkqqhp4whwq8u8zc5egprakvqxewr5pmx</code> | 
| <img src="https://raw.githubusercontent.com/ErikThiart/cryptocurrency-icons/master/32/ethereum.png" alt="Ethereum (ETH)" width="32"/> | <code>0x3147bEE3179Df0f6a0852044BFe3C59086072e12</code> |
|  <img src="https://raw.githubusercontent.com/ErikThiart/cryptocurrency-icons/master/32/tether.png" alt="USDT (TRC-20)" width="32"/>   |     <code>TKznmR65yhPt5qmYCML4tNSWFeeUkgYSEV</code>     |

___

<br>

<div align="center"><img src="media/logo.webp" alt="haskcore logo"></div>

<br>

<p align="center">A modern, lightweight standalone desktop IDE with LSP support, built with Kotlin & Compose Desktop for Haskell development</p>

<br>

<p align="center"><b>Because Haskell deserves its own IDE</b></p>

<br>

> [!NOTE]
> The application was designed using the [Reduce & Conquer](https://github.com/numq/reduce-and-conquer) architectural
> pattern

# Development

> [!NOTE]
> This is an approximate status and may change as development progresses

| 📦 Package | 🎯 Functionality        | 🏗️ Layer | ✅ Status        |
|------------|-------------------------|-----------|-----------------|
| clipboard  | Clipboard management    | Low-level | ✅ Stable        |
| compiler   | Single-file compilation | Low-level | ✅ Stable        |
| editor     | Code editing            | UI        | ❌  Planned      |
| explorer   | File navigation         | UI        | ✅   Stable      |
| filesystem | Virtual file system     | Low-level | ✅  Stable       |
| keymap     | Hotkeys                 | Core      | ❌  Planned      |
| lsp        | LSP protocol            | Core      | 🚧  In progress |
| output     | Console output          | UI        | 🚧  In progress |
| process    | Process execution       | Low-level | ✅   Stable      |
| project    | Project management      | Core      | 🚧  In progress |
| runner     | Project execution       | Core      | ❌  Planned      |
| stack      | Stack integration       | Low-level | ✅   Stable      |
| workspace  | IDE state               | Core      | 🚧  In progress |