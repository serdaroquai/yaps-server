// sendToServer({'type':'MOVE',id,x,y});
const log = console.log;

const updateGame= (() => {

	// last state (by using a closure we make sure updateGame is the only method that has access to state
	const currentState = 
		{
			'estimations':{},
			'poolUpdate':{}
		};
	
	
	return (message) => {
				
		if (message.type === "estimationsUpdate") {
						
			// get latest data
			$.post("api/status",{}, function(response, status){
		        currentState.estimations = response;
		        // notify component
				chartEstimations.updateData(JSON.parse(JSON.stringify(currentState)));
				chartEstimations.update();
		    });
		    /*

		    // get latest block chances
			$.post("api/blockChances",{}, function(response, status){
		        currentState.blockChances = response;

    			chartBlockChances.updateData(JSON.parse(JSON.stringify(currentState)));
				chartBlockChances.update();
		    });
		    */
			
		} else if (message.type === "estimationLabelsUpdate") {
			
			/*
			currentState.estimationLabels = message.payload.estimationLabels;
			chartEstimations.initializeLabels(JSON.parse(JSON.stringify(currentState)));
			chartEstimations.update();

			chartBlockChances.initializeLabels(JSON.parse(JSON.stringify(currentState)));
			chartBlockChances.update();
			*/

		} else if (message.type === "poolUpdate") {
			
			
			// get latest data
			$.post("api/poolStatus",{}, function(response, status){
		        currentState.poolUpdate = response;

    			chartPoolEstimations.updateData(JSON.parse(JSON.stringify(currentState)));
				chartPoolEstimations.update();
		    });
		    

		}
	};
	
	
})();


const makePoolEstimationsChart = (context, config) => {
	const chart = new Chart(context,config);
	
	chart.updateData = (newState) => {

		chart.data.datasets.forEach((dataset) => {
				
			//clear data
			dataset.data = [];
			var key = dataset.label;

			//for each label pull data
			chart.data.labels.forEach((label) => {
	        	
	        	if (typeof newState.poolUpdate[label] !== 'undefined') {
	        		dataset.data.push(newState.poolUpdate[label][key]);
	        	} else {
	        		dataset.data.push(0)
	        	}
    		});
		});
	}

	return chart;
}

const chartPoolEstimations = makePoolEstimationsChart(document.getElementById("poolEstimationsChart").getContext('2d'), {
    type: 'horizontalBar',
    data: {
    	labels: ["X17","PHI1612","Lyra2REv2","NeoScrypt","NIST5","Tribus","Xevan","X11Gost","Skein","Timetravel"],
		datasets: [{
			label: "estimateCurrent",
			borderColor:'blue',
			borderWidth: 1,
			data: [],
			spanGaps: true,
		},
		{
			label: "estimate24hr",
			borderColor:'red',
			borderWidth: 1,
			data: [],
			spanGaps: true,
		}]
    },
    options: {
    	scales: {
			xAxes: [{ticks: {
				suggestedMin: 0,
				suggestedMax: 0.001
			}}]
		},
    	title:{
    		display:true,
    		text:'Estimation x Hashrate'
    	}
    }
});

const makeBarChart = (context, config) => {
	const chart = new Chart(context,config);
	
	chart.updateData = (newState) => {

		chart.data.datasets.forEach((dataset) => {
			
			//clear data
			dataset.data = [];

			//for each label pull data
			chart.data.labels.forEach((label) => {
	        	
	        	if (typeof newState.estimations[label] !== 'undefined') {
	        		dataset.data.push(newState.estimations[label]);
	        	} else {
	        		dataset.data.push(0)
	        	}
    		});
		});
	}

	chart.initializeLabels = (newState) => {
		chart.data.labels = newState.estimationLabels;
	}

	return chart;
}

const chartEstimations = makeBarChart(document.getElementById("estimationsChart").getContext('2d'), {
    type: 'horizontalBar',
    data: {
    	labels: ["X17","PHI1612","Lyra2REv2","NeoScrypt","NIST5","Tribus","Xevan","X11Gost","Skein","Timetravel"],
		datasets: [{
			label: "BTC / hashrate / day",
			borderColor:'green',
			borderWidth: 1,
			data: [],
			spanGaps: true,
		}]
    },
    options: {
    	scales: {
			xAxes: [{ticks: {
				suggestedMin: 0,
				suggestedMax: 0.001
			}}]
		},
    	title:{
    		display:true,
    		text:'Estimation x Hashrate'
    	}
    }
});



/*

// initialize UI components
const chartDifficulty = makeDifficultyChart(document.getElementById("difficultyChart").getContext('2d'), {
    type: 'scatter',
    data: {
        datasets: [{
        	label:'x17',
        	fill:false,
        	borderColor:'blue',	
            data: []
        },
        {
        	label:'blake2s',
        	fill:false,
        	borderColor:'orange',	
            data: []
        },
        {
        	label:'nist5',
        	fill:false,
        	borderColor:'purple',	
            data: []
        },
        {
        	label:'phi',
        	fill:false,
        	borderColor:'yellow',	
            data: []
        },
        {
        	label:'skein',
        	fill:false,
        	borderColor:'green',	
            data: []
        },
        {
        	label:'tribus',
        	fill:false,
        	borderColor:'red',	
            data: []
        },
        {
        	label:'lyra2v2',
        	fill:false,
        	borderColor:'brown',	
            data: []
        },
        {
        	label:'neoscrypt',
        	fill:false,
        	borderColor:'grey',	
            data: []
        },
        {
        	label:'xevan',
        	fill:false,
        	borderColor:'black',	
            data: []
        }]
    },
    options: {
    	showLines: true,
    	elements: {
	        line: {
	            tension: 0
	        }
	    },
    	title:{
    		display:true,
    		text:'difficulty'
    	}
    }
});



*/



// connect to server
const sendToServer = ((endpoint, subscribeAdress, publishAddress, connectCallback, messageCallback) => {
	
	const stompClient = Stomp.over(new SockJS(endpoint));
	
	stompClient.connect({}, (frame) => {
		stompClient.subscribe(subscribeAdress, messageCallback);
		connectCallback();
	});
	
	return (message) => {
		stompClient.send(publishAddress, {}, JSON.stringify(message));
	}
	
})('/pokerNight',
	'/topic/estimations',
	'/app/message',
	() => {
		
		// send the registery Message
		sendToServer({command:'register'});

	},
	(msg) => {
		updateGame(JSON.parse(msg.body));
	}); 

//For now:
const makeDifficultyChart = (context, config) => {

	const chart = new Chart(context,config);
	
	chart.addData = (message) => {
		
		const difficulty = message.singleDiff; //since labels are algo ids
		if (difficulty != null) {
			chart.data.datasets.forEach((dataset) => {
	        	
	        	if (difficulty.name === dataset.label) {

	        		const point = {'x':difficulty.timestamp, 'y':difficulty['diff']};
	        		addNewPoint(dataset.data, point, true);

	        	}
	    	});	
		}
		
	};

	chart.initialize = (message) => {
		chart.data.datasets.forEach((dataset) => { // config (use it for updating entire data)
			const newData = message['difficulty'][dataset.label].data; //since labels are algo ids
        	if (newData != null) {
        		//clear old data
        		dataset.data = [];

        		newData.forEach((point) => {
        			addNewPoint(dataset.data, point, false);
        		});        		
        	}
		});
	};

	/*
	 adds the point to add as is if it has a different value than the last one or the second last one
	 or updates the last point with the same value with its new timestamp if it has the same value with two last points.
	 */
	const addNewPoint = (pointArr, currentPoint, shouldShift) => {
		// get last 2 points
		const slice = pointArr.slice(-2);
		const prevPoint = slice.pop();
		const prevPrevPoint = slice.pop();

		// always add if among first three elements
		if (typeof prevPoint === 'undefined' || typeof prevPrevPoint === 'undefined') {
			if (shouldShift) {
				pointArr.shift();
			}
			pointArr.push(currentPoint);
		} else {
			// no need to add a third point just adjust the prev point timestamp
			// no need to shift anything
			if (currentPoint.y === prevPoint.y && currentPoint.y === prevPrevPoint.y) {
				const modifiedPoint = pointArr.pop();
				modifiedPoint.x = currentPoint.x;
				pointArr.push(modifiedPoint);
			} else {
				//add the new point
				if (shouldShift) {
					pointArr.shift();
				}
				pointArr.push(currentPoint)
			}
		}
	}

	return chart;
}

const makeChart = (mission, context, config) => {

	const chart = new Chart(context,config);
	chart['mission'] = mission;
	
	chart.addData = (message) => {
		chart.data.datasets.forEach((dataset) => {
        	const algo = message.algoMap[dataset.label]; //since labels are algo ids
        	if (algo != null) {

        		const point = {'x':algo.timestamp, 'y':algo[chart.mission]};
        		addNewPoint(dataset.data, point, true);

        	}
    	});
	};

	chart.initialize = (message) => {
		chart.data.datasets.forEach((dataset) => { // config (use it for updating entire data)
			const newData = message[chart.mission][dataset.label].data; //since labels are algo ids
        	if (newData != null) {
        		//clear old data
        		dataset.data = [];

        		newData.forEach((point) => {
        			addNewPoint(dataset.data, point, false);
        		});        		
        	}
		});
	};

	/*
	 adds the point to add as is if it has a different value than the last one or the second last one
	 or updates the last point with the same value with its new timestamp if it has the same value with two last points.
	 */
	const addNewPoint = (pointArr, currentPoint, shouldShift) => {
		// get last 2 points
		const slice = pointArr.slice(-2);
		const prevPoint = slice.pop();
		const prevPrevPoint = slice.pop();

		// always add if among first three elements
		if (typeof prevPoint === 'undefined' || typeof prevPrevPoint === 'undefined') {
			if (shouldShift) {
				pointArr.shift();
			}
			pointArr.push(currentPoint);
		} else {
			// no need to add a third point just adjust the prev point timestamp
			// no need to shift anything
			if (currentPoint.y === prevPoint.y && currentPoint.y === prevPrevPoint.y) {
				const modifiedPoint = pointArr.pop();
				modifiedPoint.x = currentPoint.x;
				pointArr.push(modifiedPoint);
			} else {
				//add the new point
				if (shouldShift) {
					pointArr.shift();
				}
				pointArr.push(currentPoint)
			}
		}
	}

	return chart;
}


/*
const makeCard = (id, texture, parent, x, y) => { 
	
	const card = makeGameObject(
			id, 
			texture ? PIXI.utils.TextureCache[texture] : null,
			parent,
			x,
			y);
	
	card.anchor.set(0.5);
	card.displayGroup = defaultLayer;

	card.update = (newState, me) => {
		const target = newState.cards.find(x => me.id === x.sprite.id);
		if (target) {
			const {x, y} = target.sprite;
			card.target = new PIXI.Point(x,y);
		}
	};
	
	card.tick = (delta,animationSpeed) => {
		if (card.target && !card.dragging) {
			const {x, y} = card;
			const {x: tx,y: ty} = card.target;
			
			//TODO neatify this,
			
			card.x += (tx - x) * animationSpeed * delta;
			card.y += (ty - y) * animationSpeed * delta;
			
			if (Math.abs(card.x - tx) < 1 && Math.abs(card.y - ty) < 1) {
				card.position = card.target;
				delete card.target;
			}
		}
	}
	
	card.onDragStart = (event) => {

		if (!card.dragging) {
			card.data = event.data;
			card.dragging = true;
			card.displayGroup = dragLayer;
			
			card.alpha = 0.5;
			card.scale.x *= 1.1;
			card.scale.y *= 1.1;
			card.dragPoint = event.data.getLocalPosition(card);
			
			card.xInitial = card.x;
			card.yInitial = card.y;
		}
	};
	
	card.onDragMove = (event) => {
		if (card.dragging) {
			const newPosition = event.data.getLocalPosition(card.parent);
			card.x = newPosition.x - card.dragPoint.x;
			card.y = newPosition.y - card.dragPoint.y;
		}
	};

	card.onDragEnd = () => {
		
		if (card.dragging) {
			card.dragging = false;
			
			const {x, y, id} = card;
			card.target = new PIXI.Point(x,y);
			
	        // notify server
	        sendToServer({'type':'MOVE',id,x,y});
			
			card.displayGroup = defaultLayer;

//			card.x = card.xInitial;
//			card.y = card.yInitial;
			card.alpha = 1;
			card.scale.x /= 1.1;
	        card.scale.y /= 1.1;
	        
	        // set the interaction data to null
	        card.data = null;
		}
	};
	
	makeDraggable(card);
	return card;
}
*/







