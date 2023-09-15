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
        const dynamicComponents = [
            document.createElement("sk-gamestate"),
            document.createElement("sk-gamephase"),
            document.createElement("sk-players"),
            document.createElement("sk-hand"),
            document.createElement("sk-biddingform"),
            document.createElement("sk-bids"),
            document.createElement("sk-trick"),
        ]
        dynamicComponents.forEach(component => body.appendChild(component))

        const roundNumberEl = document.querySelector("#roundNumber")
        const trickNumberEl = document.querySelector("#trickNumber")
        listenToGameEvents({
            [EventType.RoundStarted]: ({roundNumber}) => {
                roundNumberEl.innerText = roundNumber
                trickNumberEl.innerText = ""
            },
            [EventType.TrickStarted]: ({trickNumber}) => {
                trickNumberEl.innerText = trickNumber
            }
        })

        socket.addEventListener("close", (event) => {
            console.error("disconnected from ws")
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
            this.replaceChildren()
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
            this.replaceChildren()
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
            this.updateBasedOnPlayers(INITIAL_STATE.waitingForMorePlayers)
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
                [EventType.GameStarted]: () => {
                    this.replaceChildren()
                    this.innerHTML = `<h2 id="gamePhase"></h2>`
                },
                [EventType.RoundStarted]: () => this.updateGamePhase(GamePhase.Bidding, "Place your bid!"),
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

    class PlayersElement extends HTMLElement {
        constructor() {
            super()
        }

        connectedCallback() {
            this.disconnectedCallback()
            this.disconnectFn = listenToGameEvents({
                [EventType.PlayerJoined]: ({ playerId }) => this.addPlayer(playerId),
            })

            this.innerHTML = `
                <h3>Players</h3>
                <ul id="players"></ul>
            `

            this.playersElement = document.querySelector("#players")
            INITIAL_STATE.players.forEach(this.addPlayer)
        }

        addPlayer = (playerId) => {
            const li = document.createElement("li")
            li.innerText = playerId
            this.playersElement.appendChild(li)
        }

        disconnectedCallback() {
            if (this.disconnectFn) this.disconnectFn()
            this.disconnectFn = undefined
        }
    }

    class TrickElement extends HTMLElement {
        constructor() {
            super()
        }

        connectedCallback() {
            this.disconnectedCallback()
            this.disconnectFn = listenToGameEvents({
                [EventType.RoundStarted]: this.initialiseTrick,
                [EventType.TrickStarted]: this.initialiseTrick,
                [EventType.CardPlayed]: ({playerId, card}) => this.addCard(playerId, card),
            })
        }

        initialiseTrick = () => {
            this.replaceChildren()
            this.innerHTML = `
                <section id="trickArea">
                    <h3>Cards in trick</h3>
                    <ul id="trick"></ul>
                </section>            
            `
        }

        addCard = (playerId, card) => {
            const li = document.createElement("li")
            li.innerText = `${playerId}:${card.name}`
            li.setAttribute("player", playerId)
            li.setAttribute("suit", card.suit)
            card.number && li.setAttribute("number", card.number)
            this.querySelector("#trick").appendChild(li);
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
    customElements.define("sk-players", PlayersElement)
    customElements.define("sk-trick", TrickElement)
}
