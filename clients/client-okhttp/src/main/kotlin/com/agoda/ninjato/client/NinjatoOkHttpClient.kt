package com.agoda.ninjato.client

import com.agoda.ninjato.http.Body
import com.agoda.ninjato.http.HttpClient
import com.agoda.ninjato.http.Request
import com.agoda.ninjato.http.Response
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody

class NinjatoOkHttpClient(
        private val client: OkHttpClient,
        requestFactory: Request.Factory? = null,
        responseFactory: Response.Factory? = null,
        config: HttpClient.() -> Unit = {}
) : HttpClient(requestFactory, responseFactory, config) {
    override fun execute(request: Request): Response {
        val builder = okhttp3.Request.Builder()
                .url(request.url)
                .method(request.method.name, request.body?.let { RequestBody.create(MediaType.parse(it.mediaType.toString()), it.asByteArray) })

        for((key, value) in request.headers) {
            for (it in value) builder.addHeader(key, it)
        }

        client.newCall(builder.build()).execute().use {
            val entity = responseFactory?.create() ?: Response()

            entity.request = request
            entity.code = it.code()
            entity.headers.putAll(it.headers().toMultimap())

            if (it.body() != null) {
                val body = it.body()!!

                val bytes = body.bytes()
                val contentType = body.contentType()

                val id = contentType?.let { type -> "${type.type()}/${type.subtype()}" } ?: ""
                val charset = contentType?.charset()

                val mediaType = com.agoda.ninjato.http.MediaType(id, charset ?: Charsets.UTF_8)

                entity.body = Body(bytes, mediaType)
            }

            return entity
        }
    }
}