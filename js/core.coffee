SZ = 20
angle = north: 0, east: 90, south: 180, west: 270
labels = fire: "!", "turn-left": "&#8634", "turn-right": "&#8634", forward: "&uarr;", backward: "&darr;", "fast-forward": "&#8648"

$.fn.addPlayer = (data) ->
	$('<div class="player"><div style="-webkit-transform: rotate('+angle[data.direction]+'deg); ">&uarr;</div></div>')
		.css
			bottom: SZ * data.y
			left: SZ * data.x
		.appendTo(@)
	return @

main = () ->
	$.get 'game-state', (state) ->
		board = $('.board').empty().addPlayer(state.me)
		$(state.others).each -> board.addPlayer(@)
		actions = $('.actions').empty()
		$(state.me.deck).each (i) ->
			$('<li/>').html(labels[@type] || @type)
				.append("<span class='badge'>#{@time}</span>")
				.data('index', i)
				.appendTo(actions)
		queue = $('.queue').empty()
		$(state.me.queue).each ->
			$('<li/>').html(labels[@type] || @type)
				.append("<span class='badge'>#{@time}</span>")
				.appendTo(queue)

jQuery ->
	$('.actions li').live 'mousedown', (ev) ->
		i = $(@).data('index')
		$.post "play-card/#{i}"
	$(window).bind 'keydown', (ev) ->
		$.post "play-card/#{ev.keyCode - 49}"
	setInterval main, 100

