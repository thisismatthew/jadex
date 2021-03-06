/**
 *  Client side implementation of puzzle game.
 */ 
angular.module('PuzzleNG', []).controller('PuzzleBoard', function($scope, $timeout, $window)
{
	//-------- constants --------
	
	/** The possible board sizes to choose from. */
	$scope.sizes	= [3,5,7,9,11];
	
	//--- constructors ---
	
	/**
	 *  Create/reset the board (called at startup).
	 */
	$scope.restart	= function restart(size)
	{
		//-------- attributes --------
		
		/** The size of the board. */
		$scope.boardsize = size;
		
		/** The boards (as array of string arrays). */
		$scope.board = [];
		
		/** The previous moves (as array of array of int array, e.g., B3->C3 = [[2,1], [2,2]]). */
		$scope.moves	= [];

		// initialize the board, only board fields get a value ("red", "white" or "empty"), other array indices remain unset.
		var size2	= Math.floor($scope.boardsize/2);
		for (var i = 0; i < $scope.boardsize; i++)
		{
			$scope.board[i] = [];
			for(var j = 0; j < $scope.boardsize; j++)
			{
				$scope.board[i][j] =
					i<size2		&& j<=size2	? "white" :
					i<=size2	&& j<size2	? "white" :
					i>size2		&& j>=size2	? "red" :
					i>=size2	&& j>size2	? "red" :
					i==size2	&& j==size2	? "empty" : "";
			}
		}
		
		console.log("app init");
		newGame(size);
	};
	
	//-------- methods --------
	
	/**
	 *  Generate the 'moveable' class.
	 */
	$scope.moveable	= function(col, row)
	{
		check	= $scope.getMove(col, row)!=null;
		return check ? "moveable" : "";
	}
	
	/**
	 *  Get the possible move of a piece, if any.
	 */
	$scope.getMove	= function(col, row)
	{
		ret	= null;
		// white can move/jump down or left
		if($scope.board[row][col]=="white")
		{
			ret	= row+1<$scope.boardsize && $scope.board[row+1][col]=="empty" ? [row+1, col]
				: col+1<$scope.boardsize && $scope.board[row][col+1]=="empty" ? [row, col+1]
				: row+2<$scope.boardsize && $scope.board[row+1][col]=="red" && $scope.board[row+2][col]=="empty" ? [row+2, col]
				: col+2<$scope.boardsize && $scope.board[row][col+1]=="red" && $scope.board[row][col+2]=="empty" ? [row, col+2] : null;
		}
		// red can move/jump up or right
		else if($scope.board[row][col]=="red")
		{
			ret	= row>0 && $scope.board[row-1][col]=="empty" ? [row-1, col]
				: col>0 && $scope.board[row][col-1]=="empty" ? [row, col-1]
				: row>1 && $scope.board[row-1][col]=="white" && $scope.board[row-2][col]=="empty" ? [row-2, col]
				: col>1 && $scope.board[row][col-1]=="white" && $scope.board[row][col-2]=="empty" ? [row, col-2] : null;
		}
		return ret;
	};
		
	/**
	 *  Perform a move/jump.
	 */
	$scope.doMove	= function(col, row)
	{
		move	= $scope.getMove(col, row);
		if(move!=null)
		{
			$scope.board[move[0]][move[1]]	= $scope.board[row][col];
			$scope.board[row][col]	= "empty";
			$scope.moves.push([[row, col], move]);
		}
	};
	
	/**
	 *   Take back the last move/jump.
	 */
	$scope.takeback	= function()
	{
		move	= $scope.moves.pop();
		if(move!=null)
		{
			$scope.board[move[0][0]][move[0][1]]	= $scope.board[move[1][0]][move[1][1]];
			$scope.board[move[1][0]][move[1][1]]	= "empty";
		}		
	};
	
	//-------- helper functions --------
	
	/**
	 *  Convert column index to column name.
	 *  @param first	When set to true or false, only names to be displayed in the first or last row are generated.
	 */
	$scope.columnName	= function(i, first)
	{
		c	= "A".charCodeAt(0)+i;
		s	= String.fromCharCode(c);
		return first==undefined ? s:
			first ? i*2<$scope.boardsize ? s : ""
			: i*2+1>=$scope.boardsize ? s : "";
	};
	
	/**
	 *  Convert row index to row name.
	 *  @param first	When set to true or false, only names to be displayed in the first or last column are generated.
	 */
	$scope.rowName	= function(i, first)
	{
		return first==undefined ? i+1:
			first ? i*2<$scope.boardsize ? i+1 : ""
			: i*2+1>=$scope.boardsize ? i+1 : "";
	};
	
	/**
	 *  Convert move array to move name.
	 */
	$scope.moveName	= function(move)
	{
		return $scope.columnName(move[0][1])+$scope.rowName(move[0][0])
			+ " -> " + $scope.columnName(move[1][1])+$scope.rowName(move[1][0]);
	};
	
//	/**
//	 *  Allow alert() being called inside angular expressions.
//	 */
//	$scope.alert = alert.bind($window);

	//-------- init --------
	
	/** Start with size 5. */
	$scope.restart(5);
});