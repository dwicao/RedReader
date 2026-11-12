/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.quantumbadger.redreader.reddit.prepared

import android.content.Context
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.quantumbadger.redreader.reddit.kthings.RedditPost
import java.io.File
import java.io.IOException

object FavouritePostStore {

	private const val FILE_NAME = "favourite_posts.json"

	private val json = Json {
		ignoreUnknownKeys = true
		isLenient = true
	}

	private val mapSerializer = MapSerializer(String.serializer(), String.serializer())

	fun serializePost(post: RedditPost): String =
		json.encodeToString(RedditPost.serializer(), post)

	fun deserializePost(raw: String): RedditPost? =
		try {
			json.decodeFromString(RedditPost.serializer(), raw)
		} catch(e: Exception) {
			null
		}

	fun serializeStore(entries: Map<String, String>): String =
		json.encodeToString(mapSerializer, entries)

	fun deserializeStore(raw: String): Map<String, String> =
		try {
			json.decodeFromString(mapSerializer, raw)
		} catch(e: Exception) {
			emptyMap()
		}

	@Synchronized
	fun isFavourite(context: Context, idAlone: String): Boolean =
		readStore(context).containsKey(idAlone)

	@Synchronized
	fun setFavourite(context: Context, post: RedditPost, favourite: Boolean) {
		val store = readStore(context).toMutableMap()
		store.remove(post.idAlone)
		if(favourite) {
			store[post.idAlone] = serializePost(post)
		}
		writeStore(context, store)
	}

	@Synchronized
	fun toggleFavourite(context: Context, post: RedditPost): Boolean {
		val store = readStore(context).toMutableMap()
		val nowFavourite = if(store.containsKey(post.idAlone)) {
			store.remove(post.idAlone)
			false
		} else {
			store[post.idAlone] = serializePost(post)
			true
		}
		writeStore(context, store)
		return nowFavourite
	}

	@Synchronized
	fun getFavourites(context: Context): List<RedditPost> =
		readStore(context).values.mapNotNull { deserializePost(it) }.reversed()

	private fun storeFile(context: Context): File = File(context.filesDir, FILE_NAME)

	private fun readStore(context: Context): Map<String, String> {
		val file = storeFile(context)
		if(!file.exists()) {
			return emptyMap()
		}
		return try {
			deserializeStore(file.readText())
		} catch(e: IOException) {
			emptyMap()
		}
	}

	private fun writeStore(context: Context, entries: Map<String, String>) {
		val file = storeFile(context)
		val tmp = File(file.parent, "$FILE_NAME.tmp")
		tmp.writeText(serializeStore(entries))
		if(!tmp.renameTo(file)) {
			file.delete()
			tmp.renameTo(file)
		}
	}
}
