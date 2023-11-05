<script lang="ts">
    import {CommandType, gameState, messageStore, playerId, players} from "./socket";

    let playerIdInput = ""

    function joinGame(e: Event) {
        e.preventDefault()
        e.stopImmediatePropagation()
        messageStore.send({type: CommandType.JoinGame, actor: playerIdInput})
    }
</script>

<div id="spinner" class="lds-ring u-hidden">
    <div></div>
    <div></div>
    <div></div>
    <div></div>
</div>

<form id="joinGame" on:submit={joinGame}>
    <label>
        Player ID:
        <input bind:value={playerIdInput} name="playerId" placeholder="Enter a name"/>
    </label>
    <input type="submit" value="Submit"/>
</form>

<h1>
    {#if $playerId}
        Game Page - {$playerId}
    {:else}
        Game Page
    {/if}
</h1>

<ul id="players">
    {#each $players as player}
        <li>{player}</li>
    {/each}
</ul>

<h2 id="gameState" data-state={$gameState}>{$gameState}</h2>

<ul>
    {#each $messageStore as message}
        <li class="message"><pre>{JSON.stringify(message)}</pre></li>
    {/each}
</ul>

<style>
    .message pre {
        white-space: break-spaces;
    }

    .u-hidden {
        display: none;
    }
</style>
