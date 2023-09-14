package com.woosuk.scott_di

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

data class Provider(
    private val module: Module,
    private val function: KFunction<*>,
) {
    val qualifierAnnotation: Annotation? = function.getQualifierAnnotation()

    val typeClass: KClass<*> = function.returnType.jvmErasure

    private var declaration: Declaration<*>? = null

    private var singletonInstance: Any? = null

    private val isSingleton: Boolean = function.hasAnnotation<Singleton>()

    private val params = function.parameters

    fun getInstance(providers: Providers): Any {
        if (isSingleton && singletonInstance != null) return singletonInstance!!
        if (declaration != null) return declaration?.invoke()
            ?: throw IllegalStateException("함수 반환값이 null입니다")
        val paramsInstances = getParamsInstances(providers)
        declaration = { function.callBy(paramsInstances) }
        singletonInstance = declaration?.invoke()
        return singletonInstance ?: throw java.lang.IllegalStateException("함수의 반환 값이 null 입니다")
    }

    // 함수의 파라미터를 DI 로 넣어주기
    private fun getParamsInstances(providers: Providers) =
        params.associateWith {
            if (it.type.jvmErasure.isSubclassOf(Module::class)) return@associateWith module
            providers.getInstance(
                it.type.jvmErasure,
                it.getQualifierAnnotation()
            )
        }
}
