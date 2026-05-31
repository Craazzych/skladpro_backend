package com.skladpro.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.skladpro.employees.model.Employee
import com.skladpro.employees.model.EmployeeRole
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.config.ApplicationConfig
import java.util.Date

class JwtService(
    secret: String,
    val issuer: String,
    val audience: String,
    val realm: String
) {
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun issueToken(employee: Employee): String {
        val now = System.currentTimeMillis()
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(employee.id)
            .withClaim(ROLE_CLAIM, employee.role.storageValue)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + TOKEN_LIFETIME_MS))
            .sign(algorithm)
    }

    fun principal(credential: JWTCredential): EmployeePrincipal? {
        val employeeId = credential.payload.subject ?: return null
        val role = EmployeeRole.fromRequest(
            credential.payload.getClaim(ROLE_CLAIM).asString().orEmpty()
        ) ?: return null
        return EmployeePrincipal(employeeId = employeeId, role = role)
    }

    companion object {
        private const val ROLE_CLAIM = "role"
        private const val TOKEN_LIFETIME_MS = 8 * 60 * 60 * 1000L

        fun fromConfig(config: ApplicationConfig): JwtService {
            return JwtService(
                secret = config.property("jwt.secret").getString(),
                issuer = config.property("jwt.issuer").getString(),
                audience = config.property("jwt.audience").getString(),
                realm = config.property("jwt.realm").getString()
            )
        }

        fun test(): JwtService {
            return JwtService(
                secret = "test-secret",
                issuer = "skladpro-test",
                audience = "skladpro-test-client",
                realm = "SkladPRO Test API"
            )
        }
    }
}

data class EmployeePrincipal(
    val employeeId: String,
    val role: EmployeeRole
)
