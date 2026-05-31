package com.skladpro.security

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond

fun Application.configureJwtAuthentication(jwtService: JwtService) {
    install(Authentication) {
        jwt(JWT_PROVIDER) {
            realm = jwtService.realm
            verifier(jwtService.verifier)
            validate { credential -> jwtService.principal(credential) }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("message" to "Требуется авторизация")
                )
            }
        }
    }
}

const val JWT_PROVIDER = "auth-jwt"
