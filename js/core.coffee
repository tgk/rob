SZ = 20
angle = north: 0, east: 90, south: 180, west: 270
labels = fire: "!", "turn-left": "&#8634", "turn-right": "&#8635", forward: "&uarr;", backward: "&darr;", "fast-forward": "&#8648"

$.fn.addPlayer = (data, clazz) ->
	$('<div class="player"><div style="-webkit-transform: rotate('+angle[data.direction]+'deg); ">&uarr;</div></div>')
		.addClass(clazz)
		.css
			bottom: SZ * data.y
			left: SZ * data.x
		.appendTo(@)
	return @

main = () ->
	$.get 'game-state', (state) ->
		board = $('.board').empty().addPlayer(state.me, "me")
		$(state.others).each -> board.addPlayer(@, "other")
		actions = $('.actions').empty()
		$(state.me.deck).each (i) ->
			$('<li/>').html(labels[@type] || @type)
				.css('background-color': "hsl(#{100 - 4 * @time}, 100%, 75%)")
				.append("<span class='badge'>#{@time}</span>")
				.data('index', i)
				.appendTo(actions)
		queue = $('.queue').empty()
		$(state.me.queue).each ->
			$('<li/>').html(labels[@type] || @type)
				.append("<span class='badge'>#{@time}</span>")
				.css('background-color': "hsl(#{100 - 4 * @time}, 100%, 75%)")
				.appendTo(queue)

jQuery ->
	$('.actions li').live 'mousedown', (ev) ->
		i = $(@).data('index')
		$.post "play-card/#{i}"
	$(window).bind 'keydown', (ev) ->
		$.post "play-card/#{ev.keyCode - 49}"
	setInterval main, 100

