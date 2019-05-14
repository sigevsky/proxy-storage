package com.sigevsky.data

import java.util.UUID

case class UploadJob(id: UUID, path: String, token: String, url: String)
