package com.zhuomo.flowlume.render

/** 动效注册表（编译期注册，运行期查询） */
object EffectRegistry {

    private val map = LinkedHashMap<String, Effect>()

    fun register(effect: Effect) {
        map[effect.id] = effect
    }

    fun get(id: String): Effect? = map[id]

    fun all(): List<Effect> = map.values.toList()

    fun byGroup(group: EffectGroup): List<Effect> = map.values.filter { it.meta.group == group }

    fun clear() = map.clear()
}
