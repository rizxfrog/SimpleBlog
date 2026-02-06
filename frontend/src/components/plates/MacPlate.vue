<style scoped>
.card {
	width: 100%;
	height: 100%;
	margin: 0 auto;
	border-radius: 8px;
	z-index: 1;
}

.tools {
	display: flex;
	align-items: center;
	padding: 9px;
}

.circle {
	padding: 0 4px;
}

.box {
	display: inline-block;
	align-items: center;
	width: 10px;
	height: 10px;
	padding: 1px;
	border-radius: 50%;
}

.red {
	background-color: #ff605c;
}

.yellow {
	background-color: #ffbd44;
}

.green {
	background-color: #00ca4e;
}
</style>

<template>
	<div class="card" :style="{ backgroundColor }">
		<div class="tools">
			<div class="circle">
				<span class="red box"></span>
			</div>
			<div class="circle">
				<span class="yellow box"></span>
			</div>
			<div class="circle">
				<span class="green box"></span>
			</div>
		</div>
		<div class="card__content">
			<slot />
		</div>
	</div>
</template>

<script setup lang="ts">
withDefaults(
	defineProps<{
		backgroundColor?: string
	}>(),
	{
		backgroundColor: '#f8fbfe'
	}
)
</script>
