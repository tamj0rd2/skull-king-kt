import {ErrorCode} from "./Socket";

export function showErrorMessage(message: string, reason?: string) {
    const el = document.querySelector("#errorMessage")!!
    el.setAttribute("errorCode", (reason ?? "unknown").toString())
    el.textContent = message
    el.classList.remove("u-hidden")
}
