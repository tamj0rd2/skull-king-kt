<script lang="ts">
    import {CommandType, waitingForServerResponse, messageStore} from "./socket";
    import {gameState, playerId, players} from "./GameState";
    import {GameState} from "./constants";
    import Spinner from "./components/Spinner.svelte";

    let playerIdInput = ""

    function joinGame(e: Event) {
        e.preventDefault()
        e.stopImmediatePropagation()
        messageStore.send({type: CommandType.JoinGame, actor: playerIdInput})
    }

    function gameStateAsText(gameState: GameState) {
        switch (gameState) {
            case GameState.WaitingForMorePlayers:
                return "Waiting for more players..."
            case GameState.WaitingToStart:
                return "Waiting to start..."
            default:
                return ""
        }
    }
</script>

{#if $waitingForServerResponse}
    <Spinner />
{/if}

{#if !$playerId}
    <h1>Game Page</h1>
    <form id="joinGame" on:submit={joinGame}>
        <label>
            Player ID:
            <input bind:value={playerIdInput} name="playerId" placeholder="Enter a name"/>
        </label>
        <input type="submit" value="Submit"/>
    </form>
{:else}
    <h1>Game Page - {$playerId}</h1>
    <h2 id="gameState" data-state={$gameState}>{gameStateAsText($gameState)}</h2>
    <h3>Players:</h3>
    <ul id="players">
        {#each $players as player}
            <li>{player}</li>
        {/each}
    </ul>
{/if}

<h4>Messages for debugging:</h4>
<ul>
    {#each $messageStore as message}
        <li class="message"><pre>{JSON.stringify(message)}</pre></li>
    {/each}
</ul>

<style>
    .message pre {
        white-space: break-spaces;
    }
</style>
