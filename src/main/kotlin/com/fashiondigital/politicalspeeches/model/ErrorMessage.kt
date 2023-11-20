package com.fashiondigital.politicalspeeches.model

import java.time.LocalDateTime


class ErrorMessage(
        val errorCode: ErrorCode?,
        val errorMessage: String?,
        val time: LocalDateTime = LocalDateTime.now()
)
