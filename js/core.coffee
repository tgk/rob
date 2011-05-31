SZ = 20
angle = north: 0, east: 90, south: 180, west: 270
labels = fire: "!", "turn-left": "&#8634", "turn-right": "&#8634", forward: "&uarr;", backward: "&darr;"
whoami = 0

$.fn.addPlayer = (data) ->
	$('<div class="player"><div style="-webkit-transform: rotate('+angle[data.direction]+'deg); ">&uarr;</div></div>')
		.css
			bottom: SZ * data.y
			left: SZ * data.x
		.appendTo(@)

main = () ->
	$.get '/game-state', (state) ->
		$('.board').empty()
			.addPlayer(state.players[0])
			.addPlayer(state.players[1])
		
		actions = $('.actions').empty()
		$(state.players[whoami].deck).each (i) ->
			$('<li/>').html(labels[@type] || @type)
				.data('index', i)
				.appendTo(actions)

$ ->
	console.debug("??")
	$('.actions li').live 'mousedown', (ev) ->
		i = $(@).data('index')
		d = '{"player-id": ' + whoami + ', "card-number": ' + i + '}'
		settings =
			url: '/play-card'
			type: "post"
			contentType: 'application/json'
			data: d
		console.debug settings
		$.ajax settings
	setInterval main, 50
