function connectToWs(socket) {
    const EventType = {
        PlayerJoined: "GameEvent$PlayerJoined",
        GameStarted: "GameEvent$GameStarted",
        RoundStarted: "GameEvent$RoundStarted",
        BidPlaced: "GameEvent$BidPlaced",
        BiddingCompleted: "GameEvent$BiddingCompleted",
        CardPlayed: "GameEvent$CardPlayed",
        TrickCompleted: "GameEvent$TrickCompleted",
        TrickStarted: "GameEvent$TrickStarted",
        GameCompleted: "GameEvent$GameCompleted",
    }
    const knownEventTypes = Object.values(EventType)

    const GameState = {
        WaitingForMorePlayers: "WaitingForMorePlayers",
        WaitingToStart: "WaitingToStart",
        InProgress: "InProgress",
        Complete: "Complete",
    }

    const GamePhase = {
        Bidding: "Bidding",
        BiddingCompleted: "BiddingCompleted",
        TrickTaking: "TrickTaking",
        TrickCompleted: "TrickCompleted",
    }

    function listenToGameEvents(gameEventsToCallbacks) {
        const listener = (event) => {
            try {
                const data = JSON.parse(event.data)
                const callback = gameEventsToCallbacks[data.type]
                if (callback) return callback(data)

                if (!knownEventTypes.includes(data.type)) {
                    socket.send(JSON.stringify({
                        type: "ClientMessage$UnhandledMessageFromServer",
                        offender: data.type,
                    }))
                    console.error(`Unknown message from server: ${data.type}`)
                }
            } catch (e) {
                socket.send(JSON.stringify({
                    stackTrace: e.stack,
                    type: "ClientMessage$Error",
                }))
                throw e
            }
        }
        socket.addEventListener("message", listener)
        return () => socket.removeEventListener("message", listener)
    }

    (function setupGame() {
        const body = document.querySelector("body")

        const gameState = document.createElement("sk-gamestate")
        body.appendChild(gameState)

        const gamePhase = document.createElement("sk-gamephase")
        body.appendChild(gamePhase)

        const hand = document.createElement("sk-hand")
        body.appendChild(hand)

        const biddingForm = document.createElement("sk-biddingform")
        body.appendChild(biddingForm)

        const bids = document.createElement("sk-bids")
        body.appendChild(bids)

        const roundNumberEl = document.querySelector("#roundNumber")
        const trickNumberEl = document.querySelector("#trickNumber")
        const trick = document.querySelector("#trick")

        socket.addEventListener("close", (event) => {
            console.error("disconnected from ws")
        })

        listenToGameEvents({
            [EventType.PlayerJoined]: ({playerId}) => {
                const players = document.querySelector("#players")
                const li = document.createElement("li")
                li.innerText = playerId
                players.appendChild(li)
            },
            [EventType.RoundStarted]: ({roundNumber}) => {
                roundNumberEl.innerText = roundNumber
                trickNumberEl.innerText = ""
                trick.replaceChildren()
            },
            [EventType.CardPlayed]: ({playerId, card}) => {
                const li = document.createElement("li")
                li.innerText = `${playerId}:${card.name}`
                li.setAttribute("player", playerId)
                li.setAttribute("suit", card.suit)
                card.number && li.setAttribute("number", card.number)
                trick.appendChild(li);
            },
            [EventType.TrickStarted]: ({trickNumber}) => {
                trickNumberEl.innerText = trickNumber
                trick.replaceChildren()
            }
        })
    })()

    class BiddingElement extends HTMLElement {
        constructor() {
            super()
        }

        connectedCallback() {
            this.disconnectedCallback()
            this.disconnectFn = listenToGameEvents({
                [EventType.RoundStarted]: this.showForm,
                [EventType.BiddingCompleted]: this.hideForm,
            })
        }

        showForm = ({roundNumber}) => {
            this.innerHTML = `
                <label>Bid <input type="number" name="bid" min="0" max="${roundNumber}"></label>
                <button id="placeBid" type="button" onclick="onBidSubmit()" disabled>Place Bid</button>
                <p id="biddingError"></p>
            `

            const placeBidBtn = this.querySelector("#placeBid");
            const bidInput = this.querySelector(`input[name="bid"]`);
            const biddingError = this.querySelector("#biddingError");

            bidInput.oninput = (e) => {
                const bid = e.target.value.replace(/[^0-9]/g, '')
                bidInput.value = bid
                if (bid >= 0 && bid <= roundNumber) {
                    placeBidBtn.disabled = false
                    biddingError.innerText = ""
                    return
                }

                placeBidBtn.disabled = true
                biddingError.innerText = `Bid must be between 0 and ${roundNumber}`
            }

            placeBidBtn.onclick = () => {
                socket.send(JSON.stringify({
                    type: "ClientMessage$BidPlaced",
                    bid: bidInput.value,
                }))
                this.hideForm()
            }
        }

        hideForm = () => this.replaceChildren()

        disconnectedCallback() {
            if (this.disconnectFn) this.disconnectFn()
            this.disconnectFn = undefined
        }
    }

    class BidsElement extends HTMLElement {
        constructor() {
            super();
            listenToGameEvents({
                [EventType.GameStarted]: ({players}) => this.initialiseForPlayers(players),
                [EventType.BidPlaced]: ({playerId}) => this.indicateThatPlayerHasBid(playerId),
                [EventType.BiddingCompleted]: ({bids}) => this.showActualBids(bids),
            })
        }

        connectedCallback() {
            this.innerHTML = `
                <section id="bidsArea">
                    <h3>Bids</h3>
                    <ul id="bids"></ul>
                </section>
            `
        }

        initialiseForPlayers = (players) => {
            const bids = this.querySelector("#bids")
            players.forEach(playerId => {
                const li = document.createElement("li")
                li.textContent = playerId
                li.setAttribute("data-playerBid", playerId)
                li.appendChild(document.createElement("span"))
                bids.appendChild(li)
            })
        }

        indicateThatPlayerHasBid = (playerId) => {
            this.querySelector(`[data-playerBid="${playerId}"] span`).innerText = ":" + "has bid"
        }

        showActualBids = (bids) => {
            this.querySelectorAll(`[data-playerBid]`).forEach(el => {
                const playerId = el.getAttribute("data-playerBid")
                const bid = bids[playerId]
                el.querySelector("span").innerText = ":" + bid
            })
        }
    }

    class HandElement extends HTMLElement {
        constructor() {
            super()
        }

        connectedCallback() {
            this.disconnectedCallback()
            this.disconnectFn = listenToGameEvents({
                [EventType.GameStarted]: this.showHand,
                [EventType.RoundStarted]: ({ cardsDealt }) => this.initialiseHand(cardsDealt),
            })
        }

        showHand = () => {
            this.innerHTML = `
                <section id="handArea">
                    <h3>Hand</h3>
                    <ul id="hand"></ul>
                </section> 
            `
        }

        initialiseHand = (cards) => {
            const hand = this.querySelector("#hand")
            cards.forEach(card => {
                const li = document.createElement("li")
                li.innerText = card.name
                li.setAttribute("suit", card.suit)
                card.number && li.setAttribute("number", card.number)

                const button = document.createElement("button")
                button.innerText = "Play"
                button.onclick = function playCard() {
                    li.remove()
                    socket.send(JSON.stringify({
                        type: "ClientMessage$CardPlayed",
                        cardName: card.name,
                    }))
                }
                li.appendChild(button)
                hand.appendChild(li)
            })
        }

        disconnectedCallback() {
            if (this.disconnectFn) this.disconnectFn()
            this.disconnectFn = undefined
        }
    }

    class GameStateElement extends HTMLElement {
        constructor() {
            super()
        }

        connectedCallback() {
            this.disconnectedCallback()

            this.disconnectFn = listenToGameEvents({
                [EventType.PlayerJoined]: ({waitingForMorePlayers}) => this.updateBasedOnPlayers(waitingForMorePlayers),
                [EventType.GameStarted]: () => this.updateGameState(GameState.InProgress),
                [EventType.GameCompleted]: () => this.updateGameState(GameState.Complete),
            })

            this.innerHTML = `<h2 id="gameState"></h2>`
            this.updateBasedOnPlayers(window.INITIAL_STATE.waitingForMorePlayers)
        }

        updateBasedOnPlayers = (waitingForMorePlayers) => {
            if (waitingForMorePlayers) this.updateGameState(GameState.WaitingForMorePlayers)
            else this.updateGameState(GameState.WaitingToStart)
        }

        updateGameState = (gameState) => {
            const gameStateEl = this.querySelector("#gameState")
            const gameStateMapping = {
                [GameState.WaitingForMorePlayers]: "Waiting for more players...",
                [GameState.WaitingToStart]: "Waiting for the game to start",
                [GameState.InProgress]: "The game has started!",
                [GameState.Complete]: "The game is over!"
            }

            const text = gameStateMapping[gameState]
            if (!text) throw new Error("Unknown game state: " + gameState)
            gameStateEl.innerText = gameStateMapping[gameState]
            gameStateEl.setAttribute("data-state", gameState)
        }

        disconnectedCallback() {
            if (this.disconnectFn) this.disconnectFn()
            this.disconnectFn = undefined
        }
    }

    class GamePhaseElement extends HTMLElement {
        constructor() {
            super()
        }

        connectedCallback() {
            this.disconnectedCallback()

            this.disconnectFn = listenToGameEvents({
                [EventType.GameStarted]: () => this.innerHTML = `<h2 id="gamePhase"></h2>`,
                [EventType.RoundStarted]: () => {
                    this.updateGamePhase(GamePhase.Bidding, "Place your bid!")
                },
                [EventType.BiddingCompleted]: () => this.updateGamePhase(GamePhase.BiddingCompleted, "Bidding completed :)"),
                [EventType.TrickStarted]: () => this.updateGamePhase(GamePhase.TrickTaking, "It's trick taking time!"),
                [EventType.TrickCompleted]: () => this.updateGamePhase(GamePhase.TrickCompleted, "Trick completed :)"),
            })
        }

        updateGamePhase = (gamePhase, text) => {
            const gamePhaseEl = this.querySelector("#gamePhase")
            gamePhaseEl.setAttribute("data-phase", gamePhase)
            gamePhaseEl.innerText = text
        }

        disconnectedCallback() {
            if (this.disconnectFn) this.disconnectFn()
            this.disconnectFn = undefined
        }
    }

    customElements.define("sk-biddingform", BiddingElement)
    customElements.define("sk-bids", BidsElement)
    customElements.define("sk-hand", HandElement)
    customElements.define("sk-gamestate", GameStateElement)
    customElements.define("sk-gamephase", GamePhaseElement)
}
